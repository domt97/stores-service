package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchProductService")
class SearchProductServiceTest {

    @Mock private StoreProductRepository repository;
    @Mock private TenantRepository tenantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreProductMapper mapper;

    @InjectMocks
    private SearchProductService searchProductService;

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
        SearchProductCmd cmd = SearchProductCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(StoreId.newStoreId())
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(tenantRepository.findByTenantId(cmd.getTenantId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws NotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        TenantId tenantId = TenantId.newTenantId();
        SearchProductCmd cmd = SearchProductCmd.builder()
                .tenantId(tenantId)
                .storeId(StoreId.newStoreId())
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenantInfo(tenantId)));
        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns mapped paged result of product review DTOs")
    void returnsMappedPagedResult_whenFound() {
        TenantId tenantId = TenantId.newTenantId();
        StoreId storeId = StoreId.newStoreId();
        SearchProductCmd cmd = SearchProductCmd.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        StoreProduct product = mock(StoreProduct.class);
        StoreProductReviewDto reviewDto = StoreProductReviewDto.builder()
                .id(UUID.randomUUID())
                .name("Product")
                .build();

        PagedResult<StoreProduct> pagedResult = PagedResult.<StoreProduct>builder()
                .content(List.of(product))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenantInfo(tenantId)));
        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(tenantId, storeId)));
        when(repository.searchProducts(any(), any(), any())).thenReturn(pagedResult);
        when(mapper.fromStoreProductToPreview(product)).thenReturn(reviewDto);

        PagedResult<StoreProductReviewDto> result = searchProductService.search(cmd, pageRequest);

        assertThat(result.getContent()).containsExactly(reviewDto);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
