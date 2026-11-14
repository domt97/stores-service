package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreCollectionId;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchProductService")
class SearchProductServiceTest {

    @Mock private StoreProductRepository repository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionRepository storeCollectionRepository;
    @Mock private StoreProductMapper mapper;

    @InjectMocks
    private SearchProductService searchProductService;


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
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(StoreId.newStoreId())
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(false);

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(storeRepository);
    }

    @Test
    @DisplayName("throws NotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(merchantId)
                .storeId(StoreId.newStoreId())
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns mapped paged result of product review DTOs")
    void returnsMappedPagedResult_whenFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(merchantId)
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

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.of(store(merchantId, storeId)));
        when(repository.searchProducts(eq(storeId), isNull(), isNull(), eq(pageRequest))).thenReturn(pagedResult);
        when(mapper.fromStoreProductToPreview(product)).thenReturn(reviewDto);

        PagedResult<StoreProductReviewDto> result = searchProductService.search(cmd, pageRequest);

        assertThat(result.getContent()).containsExactly(reviewDto);
        assertThat(result.getTotalElements()).isEqualTo(1);
        // No collection filter: the collection repository is not touched
        verifyNoInteractions(storeCollectionRepository);
    }

    @Test
    @DisplayName("notInCollectionId of a collection not in the store → NotFoundException, no search")
    void throwsException_whenExcludedCollectionNotInStore() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId collectionId = StoreCollectionId.of(UUID.randomUUID());
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(merchantId)
                .storeId(storeId)
                .notInCollectionId(collectionId)
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.of(store(merchantId, storeId)));
        when(storeCollectionRepository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(false);

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(Constants.ERROR_MSG_COLLECTION_NOT_FOUND);
        verify(repository, never()).searchProducts(any(), any(), any(), any());
    }

    @Test
    @DisplayName("store of another merchant with notInCollectionId → NotFoundException, collection never checked")
    void throwsException_whenStoreNotFound_beforeCollectionCheck() {
        MerchantId merchantId = MerchantId.newMerchantId();
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(merchantId)
                .storeId(StoreId.newStoreId())
                .notInCollectionId(StoreCollectionId.of(UUID.randomUUID()))
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchProductService.search(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);
        verifyNoInteractions(storeCollectionRepository, repository);
    }

    @Test
    @DisplayName("notInCollectionId of the store's collection is passed to the search")
    void passesExcludedCollection_whenInStore() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId collectionId = StoreCollectionId.of(UUID.randomUUID());
        SearchProductCmd cmd = SearchProductCmd.builder()
                .merchantId(merchantId)
                .storeId(storeId)
                .notInCollectionId(collectionId)
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();
        PagedResult<StoreProduct> empty = PagedResult.<StoreProduct>builder()
                .content(List.of()).pageNumber(0).pageSize(10).totalElements(0).totalPages(0).isLast(true).build();

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.of(store(merchantId, storeId)));
        when(storeCollectionRepository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);
        when(repository.searchProducts(storeId, null, collectionId, pageRequest)).thenReturn(empty);

        PagedResult<StoreProductReviewDto> result = searchProductService.search(cmd, pageRequest);

        assertThat(result.getContent()).isEmpty();
        verify(repository).searchProducts(storeId, null, collectionId, pageRequest);
    }
}
