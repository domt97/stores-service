package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.GetCollectionProductsCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCollectionProductsService")
class GetCollectionProductsServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionRepository storeCollectionRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private StoreProductMapper mapper;

    @InjectMocks
    private GetCollectionProductsService service;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final StoreCollectionId collectionId = StoreCollectionId.generateId();
    private final DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(1).pageSize(20).build();

    private final GetCollectionProductsCmd cmd = GetCollectionProductsCmd.builder()
            .merchantId(merchantId)
            .storeId(storeId)
            .storeCollectionId(collectionId)
            .build();

    @Test
    @DisplayName("store not owned by the merchant → NotFoundException, no collection or product query")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> service.getProducts(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);
        verifyNoInteractions(storeCollectionRepository, storeProductRepository, mapper);
    }

    @Test
    @DisplayName("collection not in the store → NotFoundException, no product query")
    void throwsException_whenCollectionNotInStore() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeCollectionRepository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(false);

        assertThatThrownBy(() -> service.getProducts(cmd, pageRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(Constants.ERROR_MSG_COLLECTION_NOT_FOUND);
        verifyNoInteractions(storeProductRepository, mapper);
    }

    @Test
    @DisplayName("returns the collection's products mapped to previews, keeping the paging metadata")
    void returnsMappedPage() {
        StoreProduct product = mock(StoreProduct.class);
        StoreProductReviewDto preview = StoreProductReviewDto.builder().id(UUID.randomUUID()).name("Bubble tea").build();
        PagedResult<StoreProduct> page = PagedResult.<StoreProduct>builder()
                .content(List.of(product)).pageNumber(1).pageSize(20).totalElements(21).totalPages(2).isLast(true).build();

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeCollectionRepository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);
        when(storeProductRepository.findInCollection(storeId, collectionId, pageRequest)).thenReturn(page);
        when(mapper.fromStoreProductToPreview(product)).thenReturn(preview);

        PagedResult<StoreProductReviewDto> result = service.getProducts(cmd, pageRequest);

        assertThat(result.getContent()).containsExactly(preview);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("empty collection → empty page")
    void returnsEmptyPage() {
        PagedResult<StoreProduct> empty = PagedResult.<StoreProduct>builder()
                .content(List.of()).pageNumber(1).pageSize(20).totalElements(0).totalPages(0).isLast(true).build();

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeCollectionRepository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);
        when(storeProductRepository.findInCollection(storeId, collectionId, pageRequest)).thenReturn(empty);

        PagedResult<StoreProductReviewDto> result = service.getProducts(cmd, pageRequest);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(mapper);
    }
}
