package com.dotran.example.store.application.service.impex;

import com.dotran.example.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.example.store.application.impex.parser.StoreProductImportReader;
import com.dotran.example.store.application.impex.product.ProductImportContext;
import com.dotran.example.store.application.impex.product.ProductImportData;
import com.dotran.example.store.application.impex.product.StoreProductImportHandler;
import com.dotran.example.store.application.impex.template.ImportResult;
import com.dotran.example.store.application.usecase.impex.ImportStoreProductUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

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
                .tenantId(importCmd.getTenantId())
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
