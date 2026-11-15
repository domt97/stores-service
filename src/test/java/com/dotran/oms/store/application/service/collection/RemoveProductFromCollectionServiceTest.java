package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.collection.RemoveProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import com.dotran.oms.store.domain.model.StoreCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveProductFromCollectionService")
class RemoveProductFromCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private RemoveProductFromCollectionService removeProductFromCollectionService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final StoreCollectionId collectionId = StoreCollectionId.generateId();
    private final ProductId product = ProductId.newProductId();

    private RemoveProductCollectionCmd cmd(List<ProductId> productIds) {
        return RemoveProductCollectionCmd.builder()
                .merchantId(merchantId)
                .storeId(storeId)
                .storeCollectionId(collectionId)
                .productIds(productIds)
                .build();
    }

    private StoreCollection collectionWith(ProductId... ids) {
        return StoreCollection.builder()
                .id(collectionId)
                .storeId(storeId)
                .name("Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(ids)))
                .build();
    }

    @Test
    @DisplayName("throws NotFoundException when the store does not belong to the merchant")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd(List.of(product))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("throws NotFoundException when the collection is not in this store")
    void throwsException_whenCollectionNotInStore() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(false);

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd(List.of(product))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Collection not found");

        verify(repository, never()).removeProducts(any(), any(), anyList(), any());
    }

    @Test
    @DisplayName("idempotent: a product not in the collection is sent to the delete (which skips it) and the call succeeds")
    void productNotInCollection_succeeds() {
        StoreCollection collection = collectionWith(product);
        ProductId notInCollection = ProductId.newProductId();
        StoreCollectionDto dto = StoreCollectionDto.builder().id(UUID.randomUUID()).build();
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);
        when(repository.removeProducts(eq(storeId), eq(collectionId), eq(List.of(notInCollection)), any(Instant.class))).thenReturn(collection);
        when(mapper.toCollectionDto(collection)).thenReturn(dto);

        assertThat(removeProductFromCollectionService.removeProducts(cmd(List.of(notInCollection)))).isEqualTo(dto);
    }

    @Test
    @DisplayName("throws BusinessException for an empty product list, and removes nothing")
    void throwsException_whenProductListEmpty() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd(List.of())))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).removeProducts(any(), any(), anyList(), any());
    }

    @Test
    @DisplayName("throws ValidationException when a product id is null, and removes nothing")
    void throwsException_whenProductIdNull() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd(Arrays.asList(ProductId.of(null), product))))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).removeProducts(any(), any(), anyList(), any());
    }

    @Test
    @DisplayName("removes product and returns updated DTO")
    void removesProducts_andReturnsDto() {
        StoreCollection collection = collectionWith(product);
        StoreCollection updatedCollection = collectionWith();

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Collection")
                .build();

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.existsByStoreIdAndId(storeId, collectionId)).thenReturn(true);
        when(repository.removeProducts(eq(storeId), eq(collectionId), eq(List.of(product)), any(Instant.class))).thenReturn(updatedCollection);
        when(mapper.toCollectionDto(updatedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = removeProductFromCollectionService.removeProducts(cmd(List.of(product)));

        assertThat(result).isEqualTo(expectedDto);
    }
}
