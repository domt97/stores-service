package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.oms.store.application.impex.parser.StoreProductImportReader;
import com.dotran.oms.store.application.impex.product.ProductImportContext;
import com.dotran.oms.store.application.impex.product.ProductImportData;
import com.dotran.oms.store.application.impex.product.StoreProductImportHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportProductService")
class ImportProductServiceTest {

    @Mock private StoreProductImportReader storeProductImportReader;
    @Mock private StoreProductImportHandler productImportHandler;

    @InjectMocks
    private ImportProductService importProductService;

    private ImportStoreProductCmd buildCmd() {
        return ImportStoreProductCmd.builder()
                .path("/tmp")
                .fileName("products.xlsx")
                .merchantId(MerchantId.newMerchantId())
                .storeId(StoreId.newStoreId())
                .build();
    }

    @Test
    @DisplayName("reads data, executes handler, sets timestamps and returns result")
    void executesSuccessfully_andSetsTimestamps() {
        ImportStoreProductCmd cmd = buildCmd();
        ProductImportData data = mock(ProductImportData.class);
        ImportResult handlerResult = ImportResult.builder()
                .status(ImportResult.ImportStatus.SUCCESS)
                .totalRecords(3)
                .build();

        when(storeProductImportReader.read(cmd.getPath(), cmd.getFileName())).thenReturn(data);
        when(productImportHandler.execute(any(ProductImportContext.class), eq(data))).thenReturn(handlerResult);

        ImportResult result = importProductService.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(3);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getExecutionTimeInMillis()).isNotNull();
    }

    @Test
    @DisplayName("passes merchantId and storeId from cmd to the handler context")
    void passesContextFields_toHandler() {
        ImportStoreProductCmd cmd = buildCmd();
        ProductImportData data = mock(ProductImportData.class);

        when(storeProductImportReader.read(any(), any())).thenReturn(data);
        when(productImportHandler.execute(any(), any())).thenReturn(
                ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).totalRecords(0).build());

        importProductService.execute(cmd);

        verify(productImportHandler).execute(
                argThat(ctx -> ctx.getMerchantId().equals(cmd.getMerchantId())
                        && ctx.getStoreId().equals(cmd.getStoreId())),
                eq(data)
        );
    }

    @Test
    @DisplayName("returns FAILED result with error message when handler throws an exception")
    void returnsFailed_whenHandlerThrows() {
        ImportStoreProductCmd cmd = buildCmd();
        ProductImportData data = mock(ProductImportData.class);

        when(storeProductImportReader.read(any(), any())).thenReturn(data);
        when(productImportHandler.execute(any(), any())).thenThrow(new RuntimeException("unexpected error"));

        ImportResult result = importProductService.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("unexpected error");
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
    }

    private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.Mockito.argThat(matcher);
    }
}
