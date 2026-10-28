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
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreCollectionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreCollectionPersistenceAdapter")
class StoreCollectionPersistenceAdapterTest {

    @Mock private SpringDataStoreCollectionRepository storeCollectionRepository;
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
                .updatedAt(Instant.parse("2026-10-27T10:00:00Z"))
                .build();
    }

    private ProductCollectionEntity link(StoreCollectionEntity collection, ProductId productId) {
        ProductCollectionEntity link = new ProductCollectionEntity();
        link.setCollection(collection);
        link.setProductId(productId.getValue());
        return link;
    }

    @Test
    @DisplayName("getByStoreIdAndId: looks up by collection id and store id and returns mapped domain")
    void getByStoreIdAndId_returnsDomain_whenFound() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId id = StoreCollectionId.generateId();
        StoreCollectionEntity entity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionRepository.findByIdAndStoreId(id.getValue(), storeId.getValue())).thenReturn(Optional.of(entity));
        when(storeCollectionPersistenceMapper.toStoreCollection(entity)).thenReturn(expected);

        Optional<StoreCollection> result = adapter.getByStoreIdAndId(storeId, id);

        assertThat(result).contains(expected);
        verify(storeCollectionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("existsByStoreIdAndId: checks collection id and store id together")
    void existsByStoreIdAndId_delegates() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId id = StoreCollectionId.generateId();

        when(storeCollectionRepository.existsByIdAndStoreId(id.getValue(), storeId.getValue())).thenReturn(true);

        assertThat(adapter.existsByStoreIdAndId(storeId, id)).isTrue();
        verify(storeCollectionRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("getByStoreIdAndId: returns empty when the collection is not in the store")
    void getByStoreIdAndId_returnsEmpty_whenNotInStore() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId id = StoreCollectionId.generateId();

        when(storeCollectionRepository.findByIdAndStoreId(id.getValue(), storeId.getValue())).thenReturn(Optional.empty());

        assertThat(adapter.getByStoreIdAndId(storeId, id)).isEmpty();
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
    @DisplayName("create: saves an empty collection when it has no products")
    void create_savesCollectionWithoutProducts() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity baseEntity = new StoreCollectionEntity();
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionPersistenceMapper.toBaseEntity(collection)).thenReturn(baseEntity);
        when(storeCollectionRepository.saveAndFlush(baseEntity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.create(collection);

        assertThat(result).isEqualTo(expected);
        assertThat(baseEntity.getProducts()).isEmpty();
    }

    @Test
    @DisplayName("create: builds one link row per product id of the collection")
    void create_withProductIds_linksEachProductToCollection() {
        StoreId storeId = StoreId.newStoreId();
        ProductId first = ProductId.newProductId();
        ProductId second = ProductId.newProductId();
        StoreCollection collection = StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Collection with Products")
                .status(CollectionStatus.ACTIVE)
                .productIds(List.of(first, second))
                .build();
        StoreCollectionEntity baseEntity = new StoreCollectionEntity();
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionPersistenceMapper.toBaseEntity(collection)).thenReturn(baseEntity);
        when(storeCollectionRepository.saveAndFlush(baseEntity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.create(collection);

        assertThat(result).isEqualTo(expected);
        assertThat(baseEntity.getProducts())
                .extracting(ProductCollectionEntity::getProductId)
                .containsExactly(first.getValue(), second.getValue());
        assertThat(baseEntity.getProducts()).allMatch(link -> link.getCollection() == baseEntity);
    }

    @Test
    @DisplayName("addProducts: reloads scoped by store, appends one link per id, copies updatedAt and saves")
    void addProducts_appendsAndSaves() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        ProductId productId = ProductId.newProductId();
        StoreCollectionEntity entity = new StoreCollectionEntity();
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(storeCollectionRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.addProducts(collection, List.of(productId));

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getProducts())
                .extracting(ProductCollectionEntity::getProductId)
                .containsExactly(productId.getValue());
        assertThat(entity.getUpdatedAt()).isEqualTo(collection.getUpdatedAt());
    }

    @Test
    @DisplayName("addProducts: throws NotFoundException and saves nothing when collection is not in the store")
    void addProducts_throwsNotFoundException_whenCollectionNotFound() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());

        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), collection.getStoreId().getValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.addProducts(collection, List.of(ProductId.newProductId())))
                .isInstanceOf(NotFoundException.class);

        verify(storeCollectionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("removeProducts: removes specified products, copies updatedAt and saves")
    void removeProducts_removesProductsAndSaves() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity entity = new StoreCollectionEntity();
        StoreCollectionEntity savedEntity = mock(StoreCollectionEntity.class);
        StoreCollection expected = buildCollection(storeId);
        ProductId toRemove = ProductId.newProductId();
        ProductId toKeep = ProductId.newProductId();
        entity.getProducts().add(link(entity, toRemove));
        entity.getProducts().add(link(entity, toKeep));

        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(storeCollectionRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(storeCollectionPersistenceMapper.toStoreCollection(savedEntity)).thenReturn(expected);

        StoreCollection result = adapter.removeProducts(collection, List.of(toRemove));

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getProducts())
                .extracting(ProductCollectionEntity::getProductId)
                .containsExactly(toKeep.getValue());
        assertThat(entity.getUpdatedAt()).isEqualTo(collection.getUpdatedAt());
    }

    @Test
    @DisplayName("removeProducts: returns early without saving when entity has no products")
    void removeProducts_returnsEarly_whenEntityHasNoProducts() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        StoreCollectionEntity entity = new StoreCollectionEntity();
        StoreCollection expected = buildCollection(storeId);

        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(storeCollectionPersistenceMapper.toStoreCollection(entity)).thenReturn(expected);

        StoreCollection result = adapter.removeProducts(collection, List.of(ProductId.newProductId()));

        assertThat(result).isEqualTo(expected);
        verify(storeCollectionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("removeProducts: throws NotFoundException and saves nothing when collection is not in the store")
    void removeProducts_throwsNotFoundException_whenCollectionNotFound() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());

        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), collection.getStoreId().getValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.removeProducts(collection, List.of(ProductId.newProductId())))
                .isInstanceOf(NotFoundException.class);

        verify(storeCollectionRepository, never()).saveAndFlush(any());
    }
}
