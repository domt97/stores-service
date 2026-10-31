package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.collection.AddProductCollectionCmd;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
@DisplayName("AddProductCollectionService")
class AddProductCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private AddProductCollectionService addProductCollectionService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final StoreCollectionId collectionId = StoreCollectionId.generateId();
    private final ProductId existingProduct = ProductId.newProductId();
    private final ProductId newProduct = ProductId.newProductId();

    private AddProductCollectionCmd cmd(List<ProductId> productIds) {
        return AddProductCollectionCmd.builder()
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
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(ids)))
                .build();
    }

    private void storeOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
    }

    @Test
    @DisplayName("throws NotFoundException when the store does not belong to the merchant")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(repository, storeProductRepository);
    }

    @Test
    @DisplayName("throws NotFoundException when the collection is not in this store")
    void throwsException_whenCollectionNotInStore() {
        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(eq(storeId), eq(collectionId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Collection not found");

        verify(repository, never()).addProducts(any(), anyList());
        verifyNoInteractions(storeProductRepository);
    }

    @Test
    @DisplayName("throws NotFoundException when a product is not in this store, and adds nothing")
    void throwsException_whenProductNotInStore() {
        ProductId otherStoreProduct = ProductId.newProductId();
        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collectionWith(existingProduct)));
        when(storeProductRepository.findExistingProductIds(eq(storeId), eq(List.of(newProduct, otherStoreProduct))))
                .thenReturn(Set.of(newProduct));

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct, otherStoreProduct))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found");

        verify(repository, never()).addProducts(any(), anyList());
    }

    @Test
    @DisplayName("throws BusinessException when a product already exists in the collection")
    void throwsException_whenProductAlreadyInCollection() {
        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collectionWith(existingProduct)));

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(existingProduct))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).addProducts(any(), anyList());
    }

    @Test
    @DisplayName("throws BusinessException when the product list is empty")
    void throwsException_whenProductListEmpty() {
        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collectionWith(existingProduct)));

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of())))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).addProducts(any(), anyList());
    }

    @Test
    @DisplayName("throws ValidationException when a product id is null")
    void throwsException_whenProductIdNull() {
        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collectionWith(existingProduct)));

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(Arrays.asList(newProduct, ProductId.of(null)))))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).addProducts(any(), anyList());
    }

    @Test
    @DisplayName("adds distinct products of the store and returns the DTO")
    void addsDistinctProducts_andReturnsDto() {
        StoreCollection collection = collectionWith(existingProduct);
        StoreCollection updatedCollection = collectionWith(existingProduct, newProduct);

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productCount(2)
                .build();

        storeOwnedByMerchant();
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collection));
        when(storeProductRepository.findExistingProductIds(eq(storeId), eq(List.of(newProduct))))
                .thenReturn(Set.of(newProduct));
        when(repository.addProducts(collection, List.of(newProduct))).thenReturn(updatedCollection);
        when(mapper.toCollectionDto(updatedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct, newProduct)));

        assertThat(result.getProductCount()).isEqualTo(2);
        verify(repository).addProducts(collection, List.of(newProduct));
    }
}
