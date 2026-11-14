package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import com.dotran.oms.store.domain.model.StoreCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
@DisplayName("CreateStoreCollectionService")
class CreateStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private CreateStoreCollectionService createStoreCollectionService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final ProductId productId = ProductId.newProductId();

    private CreateStoreCollectionCmd cmd(List<ProductId> productIds) {
        return CreateStoreCollectionCmd.builder()
                .merchantId(merchantId)
                .storeId(storeId)
                .name("Summer Collection")
                .productIds(productIds)
                .build();
    }

    private StoreCollection newCollection() {
        return StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Summer Collection")
                .productIds(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("throws NotFoundException when the store does not belong to the merchant")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> createStoreCollectionService.create(cmd(List.of(productId))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(storeProductRepository, repository);
    }

    @Test
    @DisplayName("throws NotFoundException when a product is not in this store (another store's or unknown)")
    void throwsException_whenProductNotInStore() {
        ProductId otherStoreProduct = ProductId.newProductId();
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeProductRepository.findExistingProductIds(eq(storeId), eq(List.of(productId, otherStoreProduct))))
                .thenReturn(Set.of(productId));

        assertThatThrownBy(() -> createStoreCollectionService.create(cmd(List.of(productId, otherStoreProduct))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("throws ValidationException when a product id is null")
    void throwsException_whenProductIdIsNull() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);

        assertThatThrownBy(() -> createStoreCollectionService.create(cmd(Arrays.asList(productId, null))))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("creates an empty collection without querying products when productIds is null")
    void createsEmptyCollection_whenProductIdsNull() {
        CreateStoreCollectionCmd cmd = cmd(null);
        StoreCollection collection = newCollection();
        StoreCollectionDto expectedDto = StoreCollectionDto.builder().id(UUID.randomUUID()).productCount(0).build();

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(mapper.fromCreateCmd(cmd)).thenReturn(collection);
        when(repository.create(collection)).thenReturn(collection);
        when(mapper.toCollectionDto(collection)).thenReturn(expectedDto);

        StoreCollectionDto result = createStoreCollectionService.create(cmd);

        assertThat(result).isEqualTo(expectedDto);
        assertThat(collection.getProductIds()).isEmpty();
        verifyNoInteractions(storeProductRepository);
    }

    @Test
    @DisplayName("creates the collection with distinct products of the same store")
    void createsCollection_withDistinctProductsOfStore() {
        CreateStoreCollectionCmd cmd = cmd(List.of(productId, productId));
        StoreCollection collection = newCollection();

        StoreCollection savedCollection = StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Summer Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(List.of(productId))
                .build();

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .storeId(storeId.getValue())
                .name("Summer Collection")
                .status(CollectionStatus.ACTIVE)
                .productCount(1)
                .build();

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(storeProductRepository.findExistingProductIds(eq(storeId), eq(List.of(productId))))
                .thenReturn(Set.of(productId));
        when(mapper.fromCreateCmd(cmd)).thenReturn(collection);
        ArgumentCaptor<StoreCollection> captor = ArgumentCaptor.forClass(StoreCollection.class);
        when(repository.create(captor.capture())).thenReturn(savedCollection);
        when(mapper.toCollectionDto(savedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = createStoreCollectionService.create(cmd);

        assertThat(result.getProductCount()).isEqualTo(1);
        assertThat(captor.getValue().getProductIds()).containsExactly(productId);
        assertThat(captor.getValue().getStatus()).isEqualTo(CollectionStatus.ACTIVE);
    }
}
