package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStoreProductDetailService")
class GetStoreProductDetailServiceTest {

    @Mock private StoreProductRepository repository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreProductMapper mapper;

    @InjectMocks
    private GetStoreProductDetailService getStoreProductDetailService;

    private MerchantInfo merchantInfo(MerchantId id) {
        return MerchantInfo.builder().id(id).code("ACME").name("ACME Corp").status("ACTIVE").build();
    }

    private Store store(MerchantId merchantId, StoreId storeId) {
        return Store.builder()
                .id(storeId)
                .merchantId(merchantId)
                .name("Store")
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("throws NotFoundException when merchant does not exist")
    void throwsException_whenMerchantNotFound() {
        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws NotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.of(merchantInfo(merchantId)));
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                merchantId.getValue(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws NotFoundException when product does not exist")
    void throwsException_whenProductNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();

        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.of(merchantInfo(merchantId)));
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(merchantId, storeId)));
        when(repository.getByStoreIdAndProductId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                merchantId.getValue(), storeId.getValue(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns product DTO when all entities exist")
    void returnsDto_whenAllExist() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();

        StoreProduct product = mock(StoreProduct.class);
        StoreProductDetailDto expectedDto = StoreProductDetailDto.builder()
                .id(productId.getValue())
                .name("Product")
                .build();

        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.of(merchantInfo(merchantId)));
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(merchantId, storeId)));
        when(repository.getByStoreIdAndProductId(any(), any())).thenReturn(Optional.of(product));
        when(mapper.fromStoreProduct(product)).thenReturn(expectedDto);

        StoreProductDetailDto result = getStoreProductDetailService.getProductById(
                merchantId.getValue(), storeId.getValue(), productId.getValue());

        assertThat(result).isEqualTo(expectedDto);
    }
}
