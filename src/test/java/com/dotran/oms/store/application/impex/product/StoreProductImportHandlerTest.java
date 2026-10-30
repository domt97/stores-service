package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreProductImportHandler")
class StoreProductImportHandlerTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private OutboxEventHelper outboxEventHelper;

    @InjectMocks
    private StoreProductImportHandler handler;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();


    private ProductImportContext context() {
        return ProductImportContext.builder().merchantId(merchantId).storeId(storeId).build();
    }

    private ProductImportData validData() {
        return dataWithSku("VND", new BigDecimal("10.00"));
    }

    private ProductImportData dataWithSku(String currency, BigDecimal price) {
        ProductImportRow product = ProductImportRow.builder()
                .reference("REF-001")
                .name("Product One")
                .description("Description")
                .build();
        ProductSKUImportRow sku = ProductSKUImportRow.builder()
                .productRef("REF-001")
                .sku(SKU.of("SKU-001"))
                .name("Variant")
                .price(price)
                .currency(currency)
                .build();
        ProductImageImportRow image = ProductImageImportRow.builder()
                .productRef("REF-001")
                .imageUrl("https://example.com/img.jpg")
                .build();
        return ProductImportData.builder()
                .products(List.of(product))
                .skus(List.of(sku))
                .images(List.of(image))
                .build();
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        private ProductImportData data(List<ProductImportRow> products, List<ProductSKUImportRow> skus, List<ProductImageImportRow> images) {
            return ProductImportData.builder().products(products).skus(skus).images(images).build();
        }

        private ProductSKUImportRow skuRow(String ref, String code) {
            return ProductSKUImportRow.builder().productRef(ref).sku(SKU.of(code)).price(new BigDecimal("10000")).currency("VND").build();
        }

        private ProductImageImportRow imageRow(String ref) {
            return ProductImageImportRow.builder().productRef(ref).imageUrl("https://example.com/img.jpg").build();
        }

        private void merchantAndStoreExist() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        }

        @Test
        @DisplayName("rejects a product reference used twice in the Product sheet, before anything is saved")
        void rejectsDuplicateProductReference() {
            merchantAndStoreExist();
            ProductImportRow p = ProductImportRow.builder().reference("REF-001").name("P1").build();

            assertThatThrownBy(() -> handler.execute(context(), data(List.of(p, p), List.of(skuRow("REF-001", "A")), List.of(imageRow("REF-001")))))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("REF-001");

            verify(storeProductRepository, never()).findSkuCodesInStore(any(), any());
            verify(storeProductRepository, never()).saveAll(any());
            verify(outboxEventRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("rejects a blank product reference in any of the three sheets, before anything is saved")
        void rejectsBlankReferences() {
            merchantAndStoreExist();
            ProductImportRow p = ProductImportRow.builder().reference("REF-001").name("P1").build();
            ProductImportRow blank = ProductImportRow.builder().reference(" ").name("P2").build();

            assertThatThrownBy(() -> handler.execute(context(), data(List.of(p, blank), List.of(skuRow("REF-001", "A")), List.of(imageRow("REF-001")))))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("Product sheet");
            ProductImportRow nullRef = ProductImportRow.builder().reference(null).name("P3").build();
            assertThatThrownBy(() -> handler.execute(context(), data(List.of(p, nullRef), List.of(skuRow("REF-001", "A")), List.of(imageRow("REF-001")))))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("Product sheet");
            assertThatThrownBy(() -> handler.execute(context(), data(List.of(p), List.of(skuRow("REF-001", "A"), skuRow(null, "B")), List.of(imageRow("REF-001")))))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("Product SKU");
            assertThatThrownBy(() -> handler.execute(context(), data(List.of(p), List.of(skuRow("REF-001", "A")), List.of(imageRow("REF-001"), imageRow("")))))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("Product Image");

            verify(storeProductRepository, never()).saveAll(any());
            verify(outboxEventRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("SKU and image rows without a product row are skipped (logged), the import still succeeds")
        void orphanRows_skipped() {
            merchantAndStoreExist();
            when(storeProductRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxEventHelper.createProductCreatedOutboxEvents(any(), any())).thenReturn(List.of());
            ProductImportRow p = ProductImportRow.builder().reference("REF-001").name("P1").build();

            ImportResult result = handler.execute(context(), data(List.of(p),
                    List.of(skuRow("REF-001", "A"), skuRow("REF-ORPHAN", "B")), List.of(imageRow("REF-001"), imageRow("REF-ORPHAN"))));

            assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
            assertThat(result.getTotalRecords()).isEqualTo(1);
            ArgumentCaptor<List<StoreProduct>> saved = ArgumentCaptor.forClass(List.class);
            verify(storeProductRepository).saveAll(saved.capture());
            assertThat(saved.getValue()).hasSize(1);
            StoreProduct product = saved.getValue().get(0);
            assertThat(product.getSkus()).extracting(s -> s.getSku().getValue()).containsExactly("A");
            assertThat(product.getImages()).hasSize(1);
        }

        @Test
        @DisplayName("a failing outbox write propagates out of execute (nothing swallows it, so the transaction rolls back)")
        void outboxFailure_propagates() {
            merchantAndStoreExist();
            when(storeProductRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxEventHelper.createProductCreatedOutboxEvents(any(), any())).thenReturn(List.of());
            doThrow(new IllegalStateException("outbox down")).when(outboxEventRepository).saveAll(any());

            assertThatThrownBy(() -> handler.execute(context(), validData())).hasMessage("outbox down");
        }

        @Test
        @DisplayName("throws NotFoundException when merchant not found")
        void throwsException_whenMerchantNotFound() {
            when(merchantRepository.existsById(merchantId)).thenReturn(false);

            assertThatThrownBy(() -> handler.execute(context(), validData()))
                    .isInstanceOf(NotFoundException.class);
            verify(storeRepository, never()).existsByMerchantIdAndStoreId(any(), any());
            verify(storeProductRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("throws NotFoundException when store not found")
        void throwsException_whenStoreNotFound() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

            assertThatThrownBy(() -> handler.execute(context(), validData()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws ValidationException when product list is empty")
        void throwsException_whenProductsEmpty() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            ProductImportData emptyData = ProductImportData.builder()
                    .products(List.of())
                    .skus(List.of())
                    .images(List.of())
                    .build();

            assertThatThrownBy(() -> handler.execute(context(), emptyData))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("throws ValidationException when a product has no matching SKU")
        void throwsException_whenMissingSku() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            ProductImportData dataWithNoSku = ProductImportData.builder()
                    .products(List.of(ProductImportRow.builder().reference("REF-001").name("P1").build()))
                    .skus(List.of())
                    .images(List.of(ProductImageImportRow.builder().productRef("REF-001").imageUrl("img.jpg").build()))
                    .build();

            assertThatThrownBy(() -> handler.execute(context(), dataWithNoSku))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SKU");
        }

        @Test
        @DisplayName("throws ValidationException when a product has no matching image")
        void throwsException_whenMissingImage() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            ProductImportData dataWithNoImage = ProductImportData.builder()
                    .products(List.of(ProductImportRow.builder().reference("REF-001").name("P1").build()))
                    .skus(List.of(ProductSKUImportRow.builder().productRef("REF-001")
                            .sku(SKU.of("SKU-001")).name("V").price(BigDecimal.TEN).currency("VND").build()))
                    .images(List.of())
                    .build();

            assertThatThrownBy(() -> handler.execute(context(), dataWithNoImage))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Image");
        }
    }

    @Nested
    @DisplayName("execute (full pipeline)")
    class Execute {

        private ProductImportData dataWithSkuCodes(String... codes) {
            ProductImportData base = validData();
            List<ProductSKUImportRow> skus = Arrays.stream(codes)
                    .map(code -> ProductSKUImportRow.builder().productRef("REF-001").sku(code == null ? null : SKU.of(code))
                            .price(new BigDecimal("10000")).currency("VND").build())
                    .toList();
            return ProductImportData.builder().products(base.getProducts()).skus(skus).images(base.getImages()).build();
        }

        @Test
        @DisplayName("rejects a blank, missing or too long SKU code, and a code repeated in the file, before anything is saved")
        void rejectsBadSkuCodes() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);

            assertThatThrownBy(() -> handler.execute(context(), dataWithSkuCodes(" "))).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> handler.execute(context(), dataWithSkuCodes((String) null))).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> handler.execute(context(), dataWithSkuCodes("X".repeat(101)))).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> handler.execute(context(), dataWithSkuCodes("A", "A"))).isInstanceOf(ValidationException.class);

            verify(storeProductRepository, never()).saveAll(any());
            verify(storeProductRepository, never()).findSkuCodesInStore(any(), any());
        }

        @Test
        @DisplayName("rejects a SKU code already used in the store (removed SKUs included) before anything is saved")
        void rejectsCodeInUse() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.findSkuCodesInStore(eq(storeId), any())).thenReturn(Set.of(SKU.of("B")));

            assertThatThrownBy(() -> handler.execute(context(), dataWithSkuCodes("A", "B")))
                    .isInstanceOf(SkuCodeInUseException.class)
                    .hasMessageContaining("B");

            verify(storeProductRepository, never()).saveAll(any());
            verify(outboxEventRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("rejects an invalid currency code or a fractional VND price before anything is saved")
        void rejectsInvalidSkuRow_beforeSaving() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);

            assertThatThrownBy(() -> handler.execute(context(), dataWithSku("US", new BigDecimal("10.00"))))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> handler.execute(context(), dataWithSku("VND", new BigDecimal("10.50"))))
                    .isInstanceOf(ValidationException.class);

            verify(storeProductRepository, never()).saveAll(any());
            verify(outboxEventRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("normalises a lower-case currency from the file to VND")
        void normalisesCurrency_fromFile() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxEventHelper.createProductCreatedOutboxEvents(any(), any())).thenReturn(List.of());

            handler.execute(context(), dataWithSku(" vnd ", new BigDecimal("10000")));

            ArgumentCaptor<List<StoreProduct>> captor = ArgumentCaptor.forClass(List.class);
            verify(storeProductRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getSkus().get(0).getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("processes valid data and returns SUCCESS result")
        void returnsSuccess_forValidData() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(outboxEventHelper.createProductCreatedOutboxEvents(any(), any())).thenReturn(List.of());

            ImportResult result = handler.execute(context(), validData());

            assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);
            assertThat(result.getTotalRecords()).isEqualTo(1);
            verify(storeProductRepository).saveAll(any());
        }

        @Test
        @DisplayName("creates outbox events for imported products in postImport")
        void createsOutboxEvents_inPostImport() {
            List<StoreProduct> savedProducts = new ArrayList<>();
            StoreProduct sp = StoreProduct.builder()
                    .id(com.dotran.oms.core.domain.id.ProductId.newProductId())
                    .storeId(storeId)
                    .name("Product One")
                    .skus(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();
            savedProducts.add(sp);

            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.saveAll(any())).thenReturn(savedProducts);
            when(outboxEventHelper.createProductCreatedOutboxEvents(any(), any())).thenReturn(List.of());

            handler.execute(context(), validData());

            verify(outboxEventHelper).createProductCreatedOutboxEvents(any(), any());
            verify(outboxEventRepository).saveAll(any());
        }

        @Test
        @DisplayName("skips outbox creation when no products were saved")
        void skipsOutboxCreation_whenNoProductsSaved() {
            when(merchantRepository.existsById(merchantId)).thenReturn(true);
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.saveAll(any())).thenReturn(List.of());

            handler.execute(context(), validData());

            verify(outboxEventRepository, never()).saveAll(any());
        }
    }
}
