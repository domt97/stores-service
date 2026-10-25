package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.domain.MerchantInfo;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private MerchantInfo merchantInfo() {
        return MerchantInfo.builder().id(merchantId).code("ACME").name("ACME Corp").status("ACTIVE").build();
    }

    private ProductImportContext context() {
        return ProductImportContext.builder().merchantId(merchantId).storeId(storeId).build();
    }

    private ProductImportData validData() {
        ProductImportRow product = ProductImportRow.builder()
                .reference("REF-001")
                .name("Product One")
                .description("Description")
                .build();
        ProductSKUImportRow sku = ProductSKUImportRow.builder()
                .productRef("REF-001")
                .sku(SKU.of("SKU-001"))
                .name("Variant")
                .price(new BigDecimal("10.00"))
                .currency("USD")
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

        @Test
        @DisplayName("throws NotFoundException when merchant not found")
        void throwsException_whenMerchantNotFound() {
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.execute(context(), validData()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws NotFoundException when store not found")
        void throwsException_whenStoreNotFound() {
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

            assertThatThrownBy(() -> handler.execute(context(), validData()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws ValidationException when product list is empty")
        void throwsException_whenProductsEmpty() {
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
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
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
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
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            ProductImportData dataWithNoImage = ProductImportData.builder()
                    .products(List.of(ProductImportRow.builder().reference("REF-001").name("P1").build()))
                    .skus(List.of(ProductSKUImportRow.builder().productRef("REF-001")
                            .sku(SKU.of("SKU-001")).name("V").price(BigDecimal.TEN).currency("USD").build()))
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

        @Test
        @DisplayName("processes valid data and returns SUCCESS result")
        void returnsSuccess_forValidData() {
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
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
            sp.initState();
            savedProducts.add(sp);

            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
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
            when(merchantRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo()));
            when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
            when(storeProductRepository.saveAll(any())).thenReturn(List.of());

            handler.execute(context(), validData());

            verify(outboxEventRepository, never()).saveAll(any());
        }
    }
}
