package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddProductCollectionService")
class AddProductCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private AddProductCollectionService addProductCollectionService;

    private final StoreCollectionId collectionId = StoreCollectionId.generateId();
    private final ProductId existingProduct = ProductId.newProductId();
    private final ProductId newProduct = ProductId.newProductId();

    private AddProductCollectionCmd cmd(List<ProductId> productIds) {
        return AddProductCollectionCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(StoreId.newStoreId())
                .storeCollectionId(collectionId)
                .productIds(productIds)
                .build();
    }

    private StoreCollection collectionWith(ProductId... ids) {
        return StoreCollection.builder()
                .id(collectionId)
                .storeId(StoreId.newStoreId())
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(ids)))
                .build();
    }

    @Test
    @DisplayName("throws NotFoundException when the collection does not exist")
    void throwsException_whenCollectionNotFound() {
        when(repository.getById(collectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws BusinessException when a product already exists in the collection")
    void throwsException_whenProductAlreadyInCollection() {
        StoreCollection collection = collectionWith(existingProduct);
        when(repository.getById(collectionId)).thenReturn(Optional.of(collection));

        assertThatThrownBy(() -> addProductCollectionService.addProductsToCollection(cmd(List.of(existingProduct))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("adds products to the collection and returns the DTO")
    void addsProducts_andReturnsDto() {
        StoreCollection collection = collectionWith(existingProduct);
        StoreCollection updatedCollection = collectionWith(existingProduct, newProduct);

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productCount(2)
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.of(collection));
        when(repository.addProducts(any(StoreCollection.class), eq(List.of(newProduct))))
                .thenReturn(updatedCollection);
        when(mapper.toCollectionDto(updatedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = addProductCollectionService.addProductsToCollection(cmd(List.of(newProduct)));

        assertThat(result.getProductCount()).isEqualTo(2);
    }
}
