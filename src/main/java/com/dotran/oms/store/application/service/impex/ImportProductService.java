package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.oms.store.application.impex.parser.StoreProductImportReader;
import com.dotran.oms.store.application.impex.product.ProductImportContext;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.StoreProductImportHandler;
import com.dotran.oms.store.application.usecase.impex.ImportStoreProductUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Excel product import. The file is read (S3 + parsing) outside any transaction; the handler validates, then writes
 * the products and their ProductCreated outbox rows in ONE transaction (TransactionTemplate in its {@code process}).
 * <p>
 * Do NOT make this class or {@link #execute} {@code @Transactional}: the catch below would then run inside the
 * transaction — an exception thrown outside a repository proxy would leave the transaction committable and the products
 * would commit without their events (the bug this layout fixes); one marked rollback-only would end in
 * UnexpectedRollbackException (500).
 * <p>
 * What the client sees:
 * <ul>
 *   <li>reader: IOException → 422, invalid sheet content (ImportValidationException) → 400, missing bucket/key and other
 *       parse errors → 500;</li>
 *   <li>import (validation, merchant/store not found, SKU code in use, database errors): 200 with status FAILED and the
 *       message — nothing was written.</li>
 * </ul>
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ImportProductService implements ImportStoreProductUseCase {

    private final StoreProductImportReader storeProductImportReader;
    private final StoreProductImportHandler productImportHandler;

    @Override
    public ImportResult execute(ImportStoreProductCmd importCmd) {
        log.info("Import Store Product - START");
        Instant startTime = Instant.now();
        ProductImportData productImportData =
                storeProductImportReader.read(importCmd.getPath(), importCmd.getFileName());

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
                    .errorMessage(e.getMessage())
                    .build();
            importResult.calculateExecutionTime();

            log.error("Import Store Product - FAILED", e);

            return importResult;
        }


    }
}
