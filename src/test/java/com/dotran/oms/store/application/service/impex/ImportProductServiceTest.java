package com.dotran.oms.store.application.service.impex;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
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
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportProductService")
class ImportProductServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreProductImportReader storeProductImportReader;
    @Mock private StoreProductImportHandler productImportHandler;

    private final ProductImportLimits limits = new ProductImportLimits();
    private ImportProductService importProductService;
    private final ImportStoreProductCmd cmd = ImportStoreProductCmd.builder()
            .fileName("products.xlsx")
            .merchantId(MerchantId.newMerchantId())
            .storeId(StoreId.newStoreId())
            .build();

    @BeforeEach
    void setUp() {
        limits.setPermits(1);
        limits.setSlotWaitSeconds(5);
        limits.setRetryAfterSeconds(7);
        importProductService = new ImportProductService(merchantRepository, storeRepository, storeProductImportReader,
                productImportHandler, limits);
    }

    private void merchantAndStoreExist() {
        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())).thenReturn(true);
    }

    @Test
    @DisplayName("checks merchant, then store, then reads the file, then runs the handler; sets timestamps")
    void executesInOrder_andSetsTimestamps() {
        merchantAndStoreExist();
        ProductImportData data = mock(ProductImportData.class);
        when(storeProductImportReader.read(cmd.getMerchantId(), cmd.getStoreId(), ImportFileName.of("products.xlsx"))).thenReturn(data);
        when(productImportHandler.execute(any(ProductImportContext.class), eq(data)))
                .thenReturn(ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).totalRecords(3).build());

        ImportResult result = importProductService.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
        assertThat(result.getTotalRecords()).isEqualTo(3);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        InOrder order = inOrder(merchantRepository, storeRepository, storeProductImportReader, productImportHandler);
        order.verify(merchantRepository).existsById(cmd.getMerchantId());
        order.verify(storeRepository).existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId());
        order.verify(storeProductImportReader).read(cmd.getMerchantId(), cmd.getStoreId(), ImportFileName.of("products.xlsx"));
        order.verify(productImportHandler).execute(
                argThat(ctx -> ctx.getMerchantId().equals(cmd.getMerchantId()) && ctx.getStoreId().equals(cmd.getStoreId())),
                eq(data));
    }

    @Test
    @DisplayName("unknown merchant → 404 before anything else: store not checked, file never read")
    void merchantNotFound_404_fileNeverRead() {
        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(false);

        assertThatThrownBy(() -> importProductService.execute(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);

        verify(storeRepository, never()).existsByMerchantIdAndStoreId(any(), any());
        verify(storeProductImportReader, never()).read(any(), any(), any());
        verify(productImportHandler, never()).execute(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/b.xlsx", "..", "x\\y.xlsx", "line\nbreak.xlsx"})
    @DisplayName("a fileName that is not a plain name → 400 before anything else: no lookup, no slot, file never read")
    void invalidFileName_400_nothingTouched(String fileName) {
        ImportStoreProductCmd bad = ImportStoreProductCmd.builder().fileName(fileName)
                .merchantId(cmd.getMerchantId()).storeId(cmd.getStoreId()).build();

        assertThatThrownBy(() -> importProductService.execute(bad)).isInstanceOf(ValidationException.class);

        verifyNoInteractions(merchantRepository, storeRepository, storeProductImportReader, productImportHandler);
        // the store was never marked as importing: a valid import of it runs
        merchantAndStoreExist();
        when(storeProductImportReader.read(any(), any(), any())).thenReturn(mock(ProductImportData.class));
        when(productImportHandler.execute(any(), any()))
                .thenReturn(ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).build());
        assertThat(importProductService.execute(cmd).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    @Test
    @DisplayName("store not found for this merchant → 404, file never read")
    void storeNotFound_404_fileNeverRead() {
        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())).thenReturn(false);

        assertThatThrownBy(() -> importProductService.execute(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);

        verify(storeProductImportReader, never()).read(any(), any(), any());
        verify(productImportHandler, never()).execute(any(), any());
    }

    @Test
    @DisplayName("file not in the store's folder → the reader's 404 propagates (not 200 FAILED), the import never starts")
    void fileNotFound_404_propagates() {
        merchantAndStoreExist();
        when(storeProductImportReader.read(any(), any(), any()))
                .thenThrow(new NotFoundException("The import file products.xlsx was not found in this store's import folder"));

        assertThatThrownBy(() -> importProductService.execute(cmd)).isInstanceOf(NotFoundException.class);

        verify(productImportHandler, never()).execute(any(), any());
    }

    @Test
    @DisplayName("a reader error propagates (keeps its HTTP status), the import never starts and the slot is released")
    void readerError_propagates_slotReleased() {
        merchantAndStoreExist();
        when(storeProductImportReader.read(cmd.getMerchantId(), cmd.getStoreId(), ImportFileName.of("products.xlsx")))
                .thenThrow(new ValidationException("too large"))
                .thenReturn(mock(ProductImportData.class));
        when(productImportHandler.execute(any(), any()))
                .thenReturn(ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).build());

        assertThatThrownBy(() -> importProductService.execute(cmd)).isInstanceOf(ValidationException.class).hasMessage("too large");
        verify(productImportHandler, never()).execute(any(), any());

        // the only slot (permits = 1) was released: the next import runs
        assertThat(importProductService.execute(cmd).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    @Test
    @DisplayName("the slot is released when the handler fails")
    void handlerError_slotReleased() {
        merchantAndStoreExist();
        when(storeProductImportReader.read(any(), any(), any())).thenReturn(mock(ProductImportData.class));
        when(productImportHandler.execute(any(), any()))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).build());

        assertThat(importProductService.execute(cmd).getStatus()).isEqualTo(ImportResult.ImportStatus.FAILED);
        assertThat(importProductService.execute(cmd).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    private ImportStoreProductCmd otherStore() {
        ImportStoreProductCmd other = ImportStoreProductCmd.builder().fileName("other.xlsx")
                .merchantId(cmd.getMerchantId()).storeId(StoreId.newStoreId()).build();
        when(storeRepository.existsByMerchantIdAndStoreId(other.getMerchantId(), other.getStoreId())).thenReturn(true);
        return other;
    }

    /** Starts an import of {@code first} whose file read blocks until {@code release}; returns once it is reading. */
    private ExecutorService startBlockedImport(CountDownLatch release) throws InterruptedException {
        CountDownLatch reading = new CountDownLatch(1);
        ProductImportData data = mock(ProductImportData.class);
        when(storeProductImportReader.read(eq(cmd.getMerchantId()), eq(cmd.getStoreId()), eq(ImportFileName.of("products.xlsx")))).thenAnswer(inv -> {
            reading.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return data;
        });
        when(productImportHandler.execute(any(), any()))
                .thenReturn(ImportResult.builder().status(ImportResult.ImportStatus.SUCCESS).build());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> importProductService.execute(cmd));
        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();
        return executor;
    }

    @Test
    @DisplayName("one import per store: a second import of the same store → 409 at once, its file never read")
    void sameStore_secondImport_409() throws Exception {
        merchantAndStoreExist();
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService first = startBlockedImport(release);
        try {
            assertThatThrownBy(() -> importProductService.execute(cmd)).isInstanceOf(StoreImportRunningException.class);
            verify(storeProductImportReader, times(1)).read(any(), any(), any());
        } finally {
            release.countDown();
            first.shutdown();
            assertThat(first.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        // the store is free again once its import ended
        when(storeProductImportReader.read(any(), any(), any())).thenReturn(mock(ProductImportData.class));
        assertThat(importProductService.execute(cmd).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    @Test
    @DisplayName("another store waits for a server slot (not rejected) and runs once the slot frees up")
    void otherStore_waitsForSlot_thenRuns() throws Exception {
        merchantAndStoreExist();
        ImportStoreProductCmd other = otherStore();
        when(storeProductImportReader.read(eq(other.getMerchantId()), eq(other.getStoreId()), eq(ImportFileName.of("other.xlsx")))).thenReturn(mock(ProductImportData.class));
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService first = startBlockedImport(release); // permits = 1: the only slot is taken
        ExecutorService second = Executors.newSingleThreadExecutor();
        try {
            Future<ImportResult> waiting = second.submit(() -> importProductService.execute(other));
            Thread.sleep(300);
            assertThat(waiting.isDone()).isFalse(); // waiting for the slot, not rejected
            verify(storeProductImportReader, never()).read(other.getMerchantId(), other.getStoreId(), ImportFileName.of("other.xlsx"));

            release.countDown();
            assertThat(waiting.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
        } finally {
            release.countDown();
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    @DisplayName("server saturated for the whole wait → ImportBusyException (503) with Retry-After; that file never read")
    void slotWaitTimeout_busy() throws Exception {
        limits.setSlotWaitSeconds(0);
        merchantAndStoreExist();
        ImportStoreProductCmd other = otherStore();
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService first = startBlockedImport(release);
        try {
            assertThatThrownBy(() -> importProductService.execute(other))
                    .isInstanceOf(ImportBusyException.class)
                    .satisfies(e -> assertThat(((ImportBusyException) e).getRetryAfterSeconds()).isEqualTo(7));
            verify(storeProductImportReader, never()).read(other.getMerchantId(), other.getStoreId(), ImportFileName.of("other.xlsx"));
        } finally {
            release.countDown();
            first.shutdown();
            assertThat(first.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        // the other store was not left marked as importing
        when(storeProductImportReader.read(eq(other.getMerchantId()), eq(other.getStoreId()), eq(ImportFileName.of("other.xlsx")))).thenReturn(mock(ProductImportData.class));
        assertThat(importProductService.execute(other).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    @Test
    @DisplayName("too many imports already waiting → 503 at once, without waiting")
    void tooManyWaiting_busyAtOnce() throws Exception {
        limits.setMaxWaiting(0);
        merchantAndStoreExist();
        ImportStoreProductCmd other = otherStore();
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService first = startBlockedImport(release);
        try {
            long start = System.nanoTime();
            assertThatThrownBy(() -> importProductService.execute(other)).isInstanceOf(ImportBusyException.class);
            assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)).isLessThan(2); // slot wait is 5 s
            verify(storeProductImportReader, never()).read(other.getMerchantId(), other.getStoreId(), ImportFileName.of("other.xlsx"));
        } finally {
            release.countDown();
            first.shutdown();
            assertThat(first.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("interrupted while waiting → 503, the interrupt flag is kept, the store is freed and no slot is leaked")
    void interruptedWhileWaiting() throws Exception {
        merchantAndStoreExist();
        ImportStoreProductCmd other = otherStore();
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService first = startBlockedImport(release);
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> importProductService.execute(other)).isInstanceOf(ImportBusyException.class);
            assertThat(Thread.interrupted()).isTrue(); // flag kept (and cleared here)
            verify(storeProductImportReader, never()).read(other.getMerchantId(), other.getStoreId(), ImportFileName.of("other.xlsx"));
        } finally {
            release.countDown();
            first.shutdown();
            assertThat(first.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        // permits = 1: the store is free again and exactly one slot exists (a leaked permit would let two run)
        when(storeProductImportReader.read(eq(other.getMerchantId()), eq(other.getStoreId()), eq(ImportFileName.of("other.xlsx")))).thenReturn(mock(ProductImportData.class));
        assertThat(importProductService.execute(other).getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
    }

    static Stream<Arguments> keptMessages() {
        return Stream.of(
                Arguments.of(new ValidationException("Product list is empty")),
                Arguments.of(new BusinessException("rule")),
                Arguments.of(new ProductRuleViolationException("An ACTIVE product needs at least one ACTIVE SKU")),
                Arguments.of(new SkuCodeInUseException("SKU code(s) already used by another product of the store: A")),
                Arguments.of(new NotFoundException("Not found")));
    }

    @ParameterizedTest
    @MethodSource("keptMessages")
    @DisplayName("FAILED keeps the message of a rule the file broke")
    void failed_keepsRuleMessage(RuntimeException error) {
        merchantAndStoreExist();
        when(storeProductImportReader.read(any(), any(), any())).thenReturn(mock(ProductImportData.class));
        when(productImportHandler.execute(any(), any())).thenThrow(error);

        ImportResult result = importProductService.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo(error.getMessage());
    }

    static Stream<Arguments> internalErrors() {
        return Stream.of(
                Arguments.of(new DataIntegrityViolationException("could not execute statement [ERROR: null value in column \"name\" "
                        + "of relation \"store_products\" violates not-null constraint] [insert into store.store_products (id) values (?)]")),
                Arguments.of(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction")),
                Arguments.of(new TransactionSystemException("Could not commit JPA transaction")),
                Arguments.of(new NullPointerException("Cannot invoke \"String.length()\"")),
                Arguments.of(new IllegalStateException("internal state")),
                Arguments.of(new RuntimeException("unexpected error")));
    }

    @ParameterizedTest
    @MethodSource("internalErrors")
    @DisplayName("FAILED shows a generic message for an internal error, never its text (no SQL)")
    void failed_genericMessage_forInternalErrors(RuntimeException error) {
        merchantAndStoreExist();
        when(storeProductImportReader.read(any(), any(), any())).thenReturn(mock(ProductImportData.class));
        when(productImportHandler.execute(any(), any())).thenThrow(error);

        ImportResult result = importProductService.execute(cmd);

        assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo(Constants.ERROR_MSG_IMPORT_FAILED)
                .doesNotContain("insert").doesNotContain("ERROR:").doesNotContain("store_products");
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("transaction boundary: no @Transactional on the import path (it would be ignored in the handler and unsafe around the catch here)")
    void transactionBoundary() throws NoSuchMethodException {
        // The handler's write transaction is a TransactionTemplate in process (StoreProductImportHandlerTest proves
        // commit/rollback). @Transactional on a template step would be ignored (called on `this`) and mislead.
        assertThat(StoreProductImportHandler.class.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(java.util.Arrays.stream(StoreProductImportHandler.class.getDeclaredMethods())
                .noneMatch(m -> m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))).isTrue();

        // A transaction around the service's catch would commit products without their events.
        assertThat(ImportProductService.class.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(ImportProductService.class.getMethod("execute", ImportStoreProductCmd.class)
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
    }
}
