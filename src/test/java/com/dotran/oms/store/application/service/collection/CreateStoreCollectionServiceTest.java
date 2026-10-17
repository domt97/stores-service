package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateStoreCollectionService")
class CreateStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private CreateStoreCollectionService createStoreCollectionService;

    private final StoreId storeId = StoreId.newStoreId();
    private final ProductId productId = ProductId.newProductId();

    private StoreCollection newCollection() {
        return StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Summer Collection")
                .productIds(new ArrayList<>())
                .build();
    }

    private StoreProduct productOwnedBy(StoreId ownerStoreId) {
        return StoreProduct.builder()
                .id(productId)
                .storeId(ownerStoreId)
                .name("Product")
                .build();
    }

    @Test
    @DisplayName("throws BusinessException when a product belongs to a different store")
    void throwsException_whenProductFromDifferentStore() {
        StoreId differentStoreId = StoreId.newStoreId();

        CreateStoreCollectionCmd cmd = CreateStoreCollectionCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(storeId)
                .name("Summer Collection")
                .productIds(List.of(productId))
                .build();

        StoreCollection collection = newCollection();
        StoreProduct foreignProduct = productOwnedBy(differentStoreId);

        when(mapper.fromCreateCmd(cmd)).thenReturn(collection);
        when(storeProductRepository.getProductsByListOfProductIds(List.of(productId)))
                .thenReturn(List.of(foreignProduct));

        assertThatThrownBy(() -> createStoreCollectionService.create(cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different store");
    }

    @Test
    @DisplayName("creates the collection with valid products from the same store")
    void createsCollection_withValidProducts() {
        CreateStoreCollectionCmd cmd = CreateStoreCollectionCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(storeId)
                .name("Summer Collection")
                .productIds(List.of(productId))
                .build();

        StoreCollection collection = newCollection();
        StoreProduct ownProduct = productOwnedBy(storeId);

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

        when(mapper.fromCreateCmd(cmd)).thenReturn(collection);
        when(storeProductRepository.getProductsByListOfProductIds(List.of(productId)))
                .thenReturn(List.of(ownProduct));
        when(repository.create(any(StoreCollection.class))).thenReturn(savedCollection);
        when(mapper.toCollectionDto(savedCollection)).thenReturn(expectedDto);

        StoreCollectionDto result = createStoreCollectionService.create(cmd);

        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.getProductCount()).isEqualTo(1);
    }
}
