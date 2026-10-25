package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.RemoveProductCollectionCmd;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveProductFromCollectionService")
class RemoveProductFromCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private RemoveProductFromCollectionService removeProductFromCollectionService;

    @Test
    @DisplayName("throws NotFoundException when collection does not exist")
    void throwsException_whenCollectionNotFound() {
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        RemoveProductCollectionCmd cmd = RemoveProductCollectionCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(StoreId.newStoreId())
                .storeCollectionId(collectionId)
                .productIds(List.of(ProductId.newProductId()))
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("throws BusinessException when product is not in collection")
    void throwsException_whenProductNotInCollection() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        ProductId productInCollection = ProductId.newProductId();
        ProductId productNotInCollection = ProductId.newProductId();

        StoreCollection collection = StoreCollection.builder()
                .id(collectionId)
                .storeId(storeId)
                .name("Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(productInCollection)))
                .build();

        RemoveProductCollectionCmd cmd = RemoveProductCollectionCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(storeId)
                .storeCollectionId(collectionId)
                .productIds(List.of(productNotInCollection))
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.of(collection));

        assertThatThrownBy(() -> removeProductFromCollectionService.removeProducts(cmd))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("removes product and returns updated DTO")
    void removesProducts_andReturnsDto() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        ProductId product = ProductId.newProductId();

        StoreCollection collection = StoreCollection.builder()
                .id(collectionId)
                .storeId(storeId)
                .name("Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(product)))
                .build();

        StoreCollection updatedCollection = StoreCollection.builder()
                .id(collectionId)
                .storeId(storeId)
                .name("Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>())
                .build();

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Collection")
                .build();

        RemoveProductCollectionCmd cmd = RemoveProductCollectionCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(storeId)
                .storeCollectionId(collectionId)
                .productIds(List.of(product))
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.of(collection));
        when(repository.removeProducts(any(StoreCollection.class), any())).thenReturn(updatedCollection);
        when(mapper.toCollectionDto(updatedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = removeProductFromCollectionService.removeProducts(cmd);

        assertThat(result).isEqualTo(expectedDto);
    }
}
