package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.TenantRepository;
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
    @Mock private TenantRepository tenantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreProductMapper mapper;

    @InjectMocks
    private GetStoreProductDetailService getStoreProductDetailService;

    private TenantInfo tenantInfo(TenantId id) {
        return TenantInfo.builder().id(id).code("ACME").name("ACME Corp").status("ACTIVE").build();
    }

    private Store store(TenantId tenantId, StoreId storeId) {
        return Store.builder()
                .id(storeId)
                .tenantId(tenantId)
                .name("Store")
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("throws NotFoundException when tenant does not exist")
    void throwsException_whenTenantNotFound() {
        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws NotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        TenantId tenantId = TenantId.newTenantId();
        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.of(tenantInfo(tenantId)));
        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                tenantId.getValue(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws NotFoundException when product does not exist")
    void throwsException_whenProductNotFound() {
        TenantId tenantId = TenantId.newTenantId();
        StoreId storeId = StoreId.newStoreId();

        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.of(tenantInfo(tenantId)));
        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(tenantId, storeId)));
        when(repository.getByStoreIdAndProductId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreProductDetailService.getProductById(
                tenantId.getValue(), storeId.getValue(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns product DTO when all entities exist")
    void returnsDto_whenAllExist() {
        TenantId tenantId = TenantId.newTenantId();
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();

        StoreProduct product = mock(StoreProduct.class);
        StoreProductDetailDto expectedDto = StoreProductDetailDto.builder()
                .id(productId.getValue())
                .name("Product")
                .build();

        when(tenantRepository.findByTenantId(any())).thenReturn(Optional.of(tenantInfo(tenantId)));
        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(tenantId, storeId)));
        when(repository.getByStoreIdAndProductId(any(), any())).thenReturn(Optional.of(product));
        when(mapper.fromStoreProduct(product)).thenReturn(expectedDto);

        StoreProductDetailDto result = getStoreProductDetailService.getProductById(
                tenantId.getValue(), storeId.getValue(), productId.getValue());

        assertThat(result).isEqualTo(expectedDto);
    }
}
