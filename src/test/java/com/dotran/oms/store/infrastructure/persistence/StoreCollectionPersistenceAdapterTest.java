package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.infrastructure.mapper.StoreCollectionPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreCollectionRepository;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreCollectionPersistenceAdapter")
class StoreCollectionPersistenceAdapterTest {

    @Mock private SpringDataStoreCollectionRepository storeCollectionRepository;
    @Mock private SpringDataStoreProductRepository storeProductRepository;
    @Mock private StoreCollectionPersistenceMapper storeCollectionPersistenceMapper;

    @InjectMocks
    private StoreCollectionPersistenceAdapter adapter;

    private StoreCollection buildCollection(StoreId storeId) {
        return StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("getById: returns mapped domain when found")
    void getById_returnsDomain_whenFound() {
        StoreCollectionId id = StoreCollectionId.generateId();
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(StoreId.newStoreId());

        when(storeCollectionRepository.findById(id.getValue())).thenReturn(Optional.of(entity));
        when(storeCollectionPersistenceMapper.toStoreCollection(entity)).thenReturn(expected);

        Optional<StoreCollection> result = adapter.getById(id);

        assertThat(result).contains(expected);
    }

    @Test
    @DisplayName("getById: returns empty when not found")
    void getById_returnsEmpty_whenNotFound() {
        StoreCollectionId id = StoreCollectionId.generateId();

        when(storeCollectionRepository.findById(id.getValue())).thenReturn(Optional.empty());

        Optional<StoreCollection> result = adapter.getById(id);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getListCollectionByStoreId: returns all collections for a store")
    void getList_returnsAllCollectionsForStore() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollection collection = buildCollection(storeId);

        when(storeCollectionRepository.findAllByStoreId(storeId.getValue())).thenReturn(List.of(entity));
        when(storeCollectionPersistenceMapper.toStoreCollection(entity)).thenReturn(collection);

        List<StoreCollection> result = adapter.getListCollectionByStoreId(storeId);

        assertThat(result).containsExactly(collection);
    }

    @Test
    @DisplayName("getListCollectionByStoreId: returns empty list when store has no collections")
    void getList_returnsEmpty_whenNone() {
        StoreId storeId = StoreId.newStoreId();

        when(storeCollectionRepository.findAllByStoreId(storeId.getValue())).thenReturn(List.of());

        List<StoreCollection> result = adapter.getListCollectionByStoreId(storeId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("create: maps, builds product links, saves, and returns domain")
    void create_savesCollectionWithProducts() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity baseEntity = mock(StoreCollectionEntity.class);
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionPersistenceMapper.toBaseEntity(collection)).thenReturn(baseEntity);
        when(storeProductRepository.findAllByIdIn(any())).thenReturn(List.of());
        when(storeCollectionRepository.saveAndFlush(baseEntity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.create(collection);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("create: iterates product entities and links each one to the collection")
    void create_withProductIds_linksEachProductToCollection() {
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();
        StoreCollection collection = StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Collection with Products")
                .status(CollectionStatus.ACTIVE)
                .productIds(List.of(productId))
                .build();
        StoreCollectionEntity baseEntity = mock(StoreCollectionEntity.class);
        StoreProductEntity productEntity = mock(StoreProductEntity.class);
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionPersistenceMapper.toBaseEntity(collection)).thenReturn(baseEntity);
        when(storeProductRepository.findAllByIdIn(any())).thenReturn(List.of(productEntity));
        when(productEntity.getId()).thenReturn(productId.getValue());
        when(storeCollectionRepository.saveAndFlush(baseEntity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.create(collection);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("addProducts: finds collection, appends products, saves, returns domain")
    void addProducts_appendsAndSaves() {
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreProductEntity productEntity = mock(StoreProductEntity.class);
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        List<ProductId> productIds = List.of(ProductId.newProductId());
        when(storeCollectionRepository.findById(collection.getId().getValue())).thenReturn(Optional.of(entity));
        when(entity.getProducts()).thenReturn(new ArrayList<>());
        when(productEntity.getId()).thenReturn(UUID.randomUUID());
        when(storeProductRepository.findAllByIdIn(any())).thenReturn(List.of(productEntity));
        when(storeCollectionRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.addProducts(collection, productIds);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("addProducts: throws NotFoundException when collection not found")
    void addProducts_throwsNotFoundException_whenCollectionNotFound() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());

        when(storeCollectionRepository.findById(collection.getId().getValue())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.addProducts(collection, List.of(ProductId.newProductId())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("removeProducts: removes specified products and saves")
    void removeProducts_removesProductsAndSaves() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);
        ProductId productId = ProductId.newProductId();

        ProductCollectionEntity productCollEntity = mock(ProductCollectionEntity.class);
        when(productCollEntity.getProductId()).thenReturn(productId.getValue());
        List<ProductCollectionEntity> products = new ArrayList<>(List.of(productCollEntity));

        when(storeCollectionRepository.findById(collection.getId().getValue())).thenReturn(Optional.of(entity));
        when(entity.getProducts()).thenReturn(products);
        when(storeCollectionRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.removeProducts(collection, List.of(productId));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("removeProducts: returns early without saving when entity has no products")
    void removeProducts_returnsEarly_whenEntityHasNoProducts() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        // entity.getProducts() returns null by default → CollectionUtils.isEmpty(null) = true → early return
        when(storeCollectionRepository.findById(collection.getId().getValue())).thenReturn(Optional.of(entity));
        when(storeCollectionPersistenceMapper.toStoreCollection(entity)).thenReturn(expected);

        StoreCollection result = adapter.removeProducts(collection, List.of(ProductId.newProductId()));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("removeProducts: throws NotFoundException when collection not found")
    void removeProducts_throwsNotFoundException_whenCollectionNotFound() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());

        when(storeCollectionRepository.findById(collection.getId().getValue())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.removeProducts(collection, List.of(ProductId.newProductId())))
                .isInstanceOf(NotFoundException.class);
    }
}
