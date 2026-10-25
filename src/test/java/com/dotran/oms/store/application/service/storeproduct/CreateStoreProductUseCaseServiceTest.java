package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateStoreProductUseCaseService")
class CreateStoreProductUseCaseServiceTest {

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private StoreProductMapper storeProductMapper;
    @Mock private OutboxEventHelper outboxEventHelper;

    @InjectMocks
    private CreateStoreProductUseCaseService createStoreProductUseCaseService;

    @Test
    @DisplayName("maps command, initializes product, saves and returns DTO")
    void createsProduct_andReturnsDto() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();

        CreateStoreProductCmd cmd = CreateStoreProductCmd.builder()
                .merchantId(merchantId)
                .storeId(storeId)
                .name("Test Product")
                .build();

        StoreProduct product = StoreProduct.builder()
                .id(productId)
                .storeId(storeId)
                .name("Test Product")
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        StoreProduct savedProduct = StoreProduct.builder()
                .id(productId)
                .storeId(storeId)
                .name("Test Product")
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();
        savedProduct.initState();

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

        when(storeProductMapper.fromCreateStoreProductCmd(cmd)).thenReturn(product);
        when(storeProductRepository.create(any(StoreProduct.class))).thenReturn(savedProduct);
        when(outboxEventHelper.createProductCreatedOutboxEvent(eq(merchantId), eq(savedProduct))).thenReturn(outboxEvent);
        when(storeProductMapper.fromStoreProduct(savedProduct)).thenReturn(expectedDto);

        StoreProductDetailDto result = createStoreProductUseCaseService.createProduct(cmd);

        assertThat(result).isEqualTo(expectedDto);
        verify(storeProductRepository).create(product);
        verify(outboxEventRepository).save(outboxEvent);
    }
}
