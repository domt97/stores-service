package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateStoreProductUseCaseService")
class CreateStoreProductUseCaseServiceTest {

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private StoreProductMapper storeProductMapper;
    @Mock private OutboxEventHelper outboxEventHelper;

    @InjectMocks
    private CreateStoreProductUseCaseService createStoreProductUseCaseService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final ProductId productId = ProductId.newProductId();

    private final CreateStoreProductCmd cmd = CreateStoreProductCmd.builder()
            .merchantId(merchantId)
            .storeId(storeId)
            .name("Test Product")
            .build();

    private StoreProduct productWithSku(String currency) {
        ProductSku sku = ProductSku.builder()
                .id(ProductSkuId.newProductSkuId())
                .productId(productId)
                .sku(SKU.of("SKU-001"))
                .price(new BigDecimal("25000"))
                .currency(currency)
                .build();

        return StoreProduct.builder()
                .id(productId)
                .storeId(storeId)
                .name("Test Product")
                .skus(new ArrayList<>(List.of(sku)))
                .images(new ArrayList<>())
                .build();
    }

    private void verifyNothingWritten() {
        verify(storeProductRepository, never()).create(any());
        verify(outboxEventRepository, never()).save(any());
        verifyNoInteractions(outboxEventHelper);
    }

    @Test
    @DisplayName("checks merchant and store, creates the product and publishes ProductCreated for the checked merchant")
    void createsProduct_andReturnsDto() {
        StoreProduct product = productWithSku("vnd");
        StoreProduct savedProduct = productWithSku("VND");

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(EventId.newEventId())
                .aggregateId(productId.getValue())
                .aggregateType("Product")
                .eventType("ProductCreated")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        StoreProductDetailDto expectedDto = StoreProductDetailDto.builder()
                .id(productId.getValue())
                .name("Test Product")
                .build();

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeProductMapper.fromCreateStoreProductCmd(cmd)).thenReturn(product);
        when(storeProductRepository.create(product)).thenReturn(savedProduct);
        when(outboxEventHelper.createProductCreatedOutboxEvent(eq(merchantId), eq(savedProduct))).thenReturn(outboxEvent);
        when(storeProductMapper.fromStoreProduct(savedProduct)).thenReturn(expectedDto);

        StoreProductDetailDto result = createStoreProductUseCaseService.createProduct(cmd);

        assertThat(result).isEqualTo(expectedDto);
        assertThat(product.getSkus().get(0).getCurrency()).isEqualTo("VND");
        verify(storeProductRepository).create(product);
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    @DisplayName("throws NotFoundException and writes nothing when the merchant does not exist")
    void throwsNotFound_whenMerchantMissing() {
        when(merchantRepository.existsById(merchantId)).thenReturn(false);

        assertThatThrownBy(() -> createStoreProductUseCaseService.createProduct(cmd))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Merchant not found");

        verifyNoInteractions(storeRepository, storeProductMapper);
        verifyNothingWritten();
    }

    @Test
    @DisplayName("throws NotFoundException and writes nothing when the store is not the merchant's (or does not exist)")
    void throwsNotFound_whenStoreNotOwnedByMerchant() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> createStoreProductUseCaseService.createProduct(cmd))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(storeProductMapper);
        verifyNothingWritten();
    }

    @Test
    @DisplayName("throws ValidationException and writes nothing when a SKU currency is not a 3-letter code")
    void throwsValidation_whenSkuCurrencyInvalid() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeProductMapper.fromCreateStoreProductCmd(cmd)).thenReturn(productWithSku("US"));

        assertThatThrownBy(() -> createStoreProductUseCaseService.createProduct(cmd))
                .isInstanceOf(ValidationException.class);

        verifyNothingWritten();
    }
}
