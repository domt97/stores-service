package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.oms.store.application.config.ProductImportLimits;
import com.dotran.oms.store.application.impex.parser.StoreProductImportReader;
import com.dotran.oms.store.application.impex.product.ImportBusyException;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.product.ProductImportContext;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.StoreImportRunningException;
import com.dotran.oms.store.application.impex.product.StoreProductImportHandler;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.impex.ImportStoreProductUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import lombok.extern.slf4j.Slf4j;

import com.dotran.oms.core.domain.id.StoreId;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Excel product import. Order: file name checked (400, BR-CAT-16: a plain name inside the store's import folder,
 * before any lookup) → merchant and store checked (404) → one import per store (409 if this store already
 * imports) → a server slot (waits up to {@code slotWaitSeconds}; 503 if the server stays saturated) → the
 * file read and checked against the limits (BR-CAT-13; 400/422/500, no transaction) → the handler validates the rows
 * and writes the products and their ProductCreated outbox rows in ONE transaction (TransactionTemplate in its
 * {@code process}).
 * <p>
 * Do NOT make this class or {@link #execute} {@code @Transactional}: the catch below would then run inside the
 * transaction — an exception thrown outside a repository proxy would leave the transaction committable and the products
 * would commit without their events; one marked rollback-only would end in UnexpectedRollbackException (500).
 * <p>
 * What the client sees:
 * <ul>
 *   <li>400 — fileName is not a plain file name (BR-CAT-16);</li>
 *   <li>404 — merchant not found, or store not found for this merchant, or the file is not in the store's import folder;</li>
 *   <li>409 — an import for this store is already running (other stores are not affected);</li>
 *   <li>503 — every server slot stayed taken for the whole wait (Retry-After): capacity, not the client;</li>
 *   <li>400 / 422 / 500 — from the reader (see ProductExcelReader);</li>
 *   <li>200 SUCCESS, or 200 FAILED with nothing written: the rule that failed (validation, SKU code in use…), or a
 *       generic message for an internal error, whose details are only logged.</li>
 * </ul>
 */
@UseCase
@Slf4j
public class ImportProductService implements ImportStoreProductUseCase {

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreProductImportReader storeProductImportReader;
    private final StoreProductImportHandler productImportHandler;
    private final ProductImportLimits limits;
    private final Semaphore slots;
    /** Stores importing right now on this instance (another instance is backstopped by uk_product_skus_store_sku). */
    private final Set<StoreId> runningStores = ConcurrentHashMap.newKeySet();

    public ImportProductService(MerchantRepository merchantRepository,
                                StoreRepository storeRepository,
                                StoreProductImportReader storeProductImportReader,
                                StoreProductImportHandler productImportHandler,
                                ProductImportLimits limits) {
        this.merchantRepository = merchantRepository;
        this.storeRepository = storeRepository;
        this.storeProductImportReader = storeProductImportReader;
        this.productImportHandler = productImportHandler;
        this.limits = limits;
        int permits = limits.effectivePermits(Runtime.getRuntime().maxMemory());
        this.slots = new Semaphore(permits, true);
        log.info("Product import: {} slots on this instance", permits);
    }

    @Override
    public ImportResult execute(ImportStoreProductCmd importCmd) {
        log.info("Import Store Product - START");
        ImportFileName fileName = ImportFileName.of(importCmd.getFileName());
        if (!merchantRepository.existsById(importCmd.getMerchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }
        if (!storeRepository.existsByMerchantIdAndStoreId(importCmd.getMerchantId(), importCmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }
        if (!runningStores.add(importCmd.getStoreId())) {
            log.warn("Import rejected, store already importing: merchant={} store={}",
                    importCmd.getMerchantId().getValue(), importCmd.getStoreId().getValue());
            throw new StoreImportRunningException();
        }
        try {
            acquireSlot(importCmd);
            try {
                return importFile(importCmd, fileName);
            } finally {
                slots.release();
            }
        } finally {
            runningStores.remove(importCmd.getStoreId());
        }
    }

    /** Waits for a free server slot (fair: first come, first served); 503 if none frees up in time or too many wait. */
    private void acquireSlot(ImportStoreProductCmd importCmd) {
        if (slots.availablePermits() == 0 && slots.getQueueLength() >= limits.getMaxWaiting()) {
            log.warn("Import rejected, {} imports already waiting for a slot: merchant={} store={}",
                    slots.getQueueLength(), importCmd.getMerchantId().getValue(), importCmd.getStoreId().getValue());
            throw new ImportBusyException(limits.getRetryAfterSeconds());
        }
        boolean acquired;
        try {
            acquired = slots.tryAcquire(limits.getSlotWaitSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquired = false;
        }
        if (!acquired) {
            log.warn("Import rejected, all import slots stayed busy for {} s: merchant={} store={}",
                    limits.getSlotWaitSeconds(), importCmd.getMerchantId().getValue(), importCmd.getStoreId().getValue());
            throw new ImportBusyException(limits.getRetryAfterSeconds());
        }
    }

    private ImportResult importFile(ImportStoreProductCmd importCmd, ImportFileName fileName) {
        Instant startTime = Instant.now();
        ProductImportData productImportData =
                storeProductImportReader.read(importCmd.getMerchantId(), importCmd.getStoreId(), fileName);

        ProductImportContext context = ProductImportContext.builder()
                .merchantId(importCmd.getMerchantId())
                .storeId(importCmd.getStoreId())
                .build();

        try {
            ImportResult importResult = productImportHandler.execute(context, productImportData);
            importResult.setStartedAt(startTime);
            importResult.setCompletedAt(Instant.now());
            importResult.calculateExecutionTime();

            return importResult;
        } catch (Exception e) {
            ImportResult importResult = ImportResult.builder()
                    .status(ImportResult.ImportStatus.FAILED)
                    .startedAt(startTime)
                    .completedAt(Instant.now())
                    .errorMessage(clientMessage(e))
                    .build();
            importResult.calculateExecutionTime();

            if (isRuleViolation(e)) {
                log.warn("Import Store Product - FAILED: merchant={} store={} file={}: {}", importCmd.getMerchantId().getValue(),
                        importCmd.getStoreId().getValue(), importCmd.getFileName(), e.getMessage());
            } else {
                log.error("Import Store Product - FAILED: merchant={} store={} file={}", importCmd.getMerchantId().getValue(),
                        importCmd.getStoreId().getValue(), importCmd.getFileName(), e);
            }

            return importResult;
        }
    }

    /** Messages written for the client are kept; anything else (database, framework, bugs) may carry internals. */
    private static String clientMessage(Exception e) {
        return isRuleViolation(e) ? e.getMessage() : Constants.ERROR_MSG_IMPORT_FAILED;
    }

    private static boolean isRuleViolation(Exception e) {
        return e instanceof ValidationException || e instanceof BusinessException || e instanceof NotFoundException
                || e instanceof SkuCodeInUseException;
    }
}
