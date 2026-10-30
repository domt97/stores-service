package com.dotran.oms.store.application.service.impex;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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

    @Test
    @DisplayName("a reader error propagates (keeps its HTTP status) and the import is never started")
    void readerError_propagates_handlerNeverCalled() {
        ImportStoreProductCmd cmd = buildCmd();
        when(storeProductImportReader.read(cmd.getPath(), cmd.getFileName())).thenThrow(new IllegalStateException("bucket missing"));

        assertThatThrownBy(() -> importProductService.execute(cmd)).hasMessage("bucket missing");

        verify(productImportHandler, never()).execute(any(), any());
    }

    @Test
    @DisplayName("transaction boundary: the handler's execute is the public @Transactional entry point; this service is not transactional")
    void transactionBoundary() throws NoSuchMethodException {
        java.lang.reflect.Method handlerExecute = StoreProductImportHandler.class.getMethod("execute", ProductImportContext.class, ProductImportData.class);
        assertThat(handlerExecute.getDeclaringClass()).isEqualTo(StoreProductImportHandler.class);
        assertThat(handlerExecute.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(handlerExecute.getModifiers())).isFalse();
        assertThat(java.lang.reflect.Modifier.isFinal(StoreProductImportHandler.class.getModifiers())).isFalse();
        org.springframework.transaction.annotation.Transactional tx =
                handlerExecute.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRED);
        assertThat(tx.readOnly()).isFalse();
        assertThat(tx.noRollbackFor()).isEmpty();
        assertThat(tx.noRollbackForClassName()).isEmpty();
        // process/postImport run on `this` (template), so an annotation there would do nothing and mislead.
        for (String name : java.util.List.of("process", "postImport", "validate", "transform")) {
            assertThat(java.util.Arrays.stream(StoreProductImportHandler.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name))
                    .noneMatch(m -> m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)))
                    .as(name).isTrue();
        }

        // A transaction around the service's catch would commit products without their events.
        assertThat(ImportProductService.class.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(ImportProductService.class.getMethod("execute", ImportStoreProductCmd.class)
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
    }
}
