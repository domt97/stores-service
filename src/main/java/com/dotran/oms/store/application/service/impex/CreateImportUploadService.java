package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.impex.CreateImportUploadCmd;
import com.dotran.oms.store.application.config.ProductImportLimits;
import com.dotran.oms.store.application.dto.impex.ImportUploadDto;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.upload.ImportUploadSigner;
import com.dotran.oms.store.application.impex.upload.SignedUpload;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.impex.CreateImportUploadUseCase;
import com.dotran.oms.store.common.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Presigned upload of a product import file (BR-CAT-17). Order: size 1..maxBytes (400) → merchant (404) → store of the
 * merchant (404) → server-chosen name {@code {uuid}.xlsx} → signed PUT into the store's import folder, valid
 * {@code uploadUrlTtl}. Nothing is written: no transaction. The URL is never logged.
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
public class CreateImportUploadService implements CreateImportUploadUseCase {

    private static final String XLSX = ".xlsx";

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final ImportUploadSigner importUploadSigner;
    private final ProductImportLimits limits;

    @Override
    public ImportUploadDto create(CreateImportUploadCmd cmd) {
        if (cmd.sizeBytes() < 1 || cmd.sizeBytes() > limits.getMaxBytes()) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_UPLOAD_SIZE_INVALID, limits.getMaxBytes()));
        }
        if (!merchantRepository.existsById(cmd.merchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.merchantId(), cmd.storeId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }
        ImportFileName fileName = ImportFileName.of(UUID.randomUUID() + XLSX);
        SignedUpload upload = importUploadSigner.sign(cmd.merchantId(), cmd.storeId(), fileName, cmd.sizeBytes(),
                limits.getUploadUrlTtl());
        log.info("Import upload URL issued: fileName={} sizeBytes={} expiresAt={}", fileName, cmd.sizeBytes(),
                upload.expiresAt());
        return new ImportUploadDto(fileName.value(), upload.url(), upload.method(), upload.headers(), upload.expiresAt(),
                limits.getMaxBytes());
    }
}
