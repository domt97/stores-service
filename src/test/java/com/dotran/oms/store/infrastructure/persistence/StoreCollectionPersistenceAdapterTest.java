package com.dotran.oms.store.infrastructure.persistence;

import java.util.UUID;
import com.dotran.oms.store.domain.exception.CollectionChangedConcurrentlyException;
import org.springframework.dao.CannotAcquireLockException;
import org.mockito.InOrder;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
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

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    @Test
    @DisplayName("addProducts: one native insert with all ids (scoped by store), touches updated_at when rows were added, re-reads")
    void addProducts_insertsTouchesAndReReads() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        UUID id = collection.getId().getValue();
        ProductId p1 = ProductId.newProductId();
        ProductId p2 = ProductId.newProductId();
        StoreCollectionEntity reRead = new StoreCollectionEntity();
        StoreCollection expected = buildCollection(storeId);
        when(storeCollectionRepository.insertProductsSkippingExisting(id, storeId.getValue(), List.of(p1.getValue(), p2.getValue()), NOW)).thenReturn(1);
        when(storeCollectionRepository.findByIdAndStoreId(id, storeId.getValue())).thenReturn(Optional.of(reRead));
        when(storeCollectionPersistenceMapper.toStoreCollection(reRead)).thenReturn(expected);

        assertThat(adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(p1, p2), NOW)).isEqualTo(expected);

        InOrder order = inOrder(storeCollectionRepository);
        order.verify(storeCollectionRepository).insertProductsSkippingExisting(id, storeId.getValue(), List.of(p1.getValue(), p2.getValue()), NOW);
        order.verify(storeCollectionRepository).touchUpdatedAt(id, storeId.getValue(), NOW);
        order.verify(storeCollectionRepository).findByIdAndStoreId(id, storeId.getValue()); // only the re-read
        verify(storeCollectionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("addProducts: nothing inserted (all already there, or added by a concurrent request) → updated_at untouched, re-read")
    void addProducts_nothingInserted_noTouch() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        UUID id = collection.getId().getValue();
        when(storeCollectionRepository.insertProductsSkippingExisting(eq(id), eq(storeId.getValue()), anyList(), eq(NOW))).thenReturn(0);
        when(storeCollectionRepository.findByIdAndStoreId(id, storeId.getValue())).thenReturn(Optional.of(new StoreCollectionEntity()));
        when(storeCollectionPersistenceMapper.toStoreCollection(any())).thenReturn(collection);

        adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(ProductId.newProductId()), NOW);

        verify(storeCollectionRepository, never()).touchUpdatedAt(any(), any(), any());
    }

    @Test
    @DisplayName("add/remove with an empty list never run the native statement (IN () is invalid SQL)")
    void emptyList_noNativeStatement() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        when(storeCollectionRepository.findByIdAndStoreId(collection.getId().getValue(), storeId.getValue()))
                .thenReturn(Optional.of(new StoreCollectionEntity()));
        when(storeCollectionPersistenceMapper.toStoreCollection(any())).thenReturn(collection);

        adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(), NOW);
        adapter.removeProducts(collection.getStoreId(), collection.getId(), List.of(), NOW);

        verify(storeCollectionRepository, never()).insertProductsSkippingExisting(any(), any(), anyList(), any());
        verify(storeCollectionRepository, never()).deleteProducts(any(), any(), anyList());
        verify(storeCollectionRepository, never()).touchUpdatedAt(any(), any(), any());
    }

    @Test
    @DisplayName("removeProducts: one native delete (scoped by store), touches updated_at only when rows were removed, re-reads")
    void removeProducts_deletesTouchesAndReReads() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollection collection = buildCollection(storeId);
        UUID id = collection.getId().getValue();
        ProductId p1 = ProductId.newProductId();
        when(storeCollectionRepository.deleteProducts(id, storeId.getValue(), List.of(p1.getValue()))).thenReturn(1, 0);
        when(storeCollectionRepository.findByIdAndStoreId(id, storeId.getValue())).thenReturn(Optional.of(new StoreCollectionEntity()));
        when(storeCollectionPersistenceMapper.toStoreCollection(any())).thenReturn(collection);

        adapter.removeProducts(collection.getStoreId(), collection.getId(), List.of(p1), NOW);
        verify(storeCollectionRepository).touchUpdatedAt(id, storeId.getValue(), NOW);

        adapter.removeProducts(collection.getStoreId(), collection.getId(), List.of(p1), NOW); // already gone: nothing deleted, no touch
        verify(storeCollectionRepository, org.mockito.Mockito.times(1)).touchUpdatedAt(id, storeId.getValue(), NOW);
        verify(storeCollectionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a deadlock or lock failure in the statement → CollectionChangedConcurrentlyException (409), no re-read")
    void lockFailure_409() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());
        when(storeCollectionRepository.insertProductsSkippingExisting(any(), any(), anyList(), any()))
                .thenThrow(new CannotAcquireLockException("deadlock detected"));
        when(storeCollectionRepository.deleteProducts(any(), any(), anyList()))
                .thenThrow(new CannotAcquireLockException("deadlock detected"));

        assertThatThrownBy(() -> adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(ProductId.newProductId()), NOW))
                .isInstanceOf(CollectionChangedConcurrentlyException.class);
        assertThatThrownBy(() -> adapter.removeProducts(collection.getStoreId(), collection.getId(), List.of(ProductId.newProductId()), NOW))
                .isInstanceOf(CollectionChangedConcurrentlyException.class);
        verify(storeCollectionRepository, never()).findByIdAndStoreId(any(), any());
    }

    @Test
    @DisplayName("a lock failure while touching updated_at → CollectionChangedConcurrentlyException (409) as well")
    void lockFailureOnTouch_409() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());
        when(storeCollectionRepository.insertProductsSkippingExisting(any(), any(), anyList(), any())).thenReturn(1);
        when(storeCollectionRepository.touchUpdatedAt(any(), any(), any())).thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(ProductId.newProductId()), NOW))
                .isInstanceOf(CollectionChangedConcurrentlyException.class);
    }

    @Test
    @DisplayName("the collection disappearing before the re-read → NotFoundException")
    void reReadMissing_404() {
        StoreCollection collection = buildCollection(StoreId.newStoreId());
        when(storeCollectionRepository.insertProductsSkippingExisting(any(), any(), anyList(), any())).thenReturn(0);
        when(storeCollectionRepository.findByIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.addProducts(collection.getStoreId(), collection.getId(), List.of(ProductId.newProductId()), NOW))
                .isInstanceOf(NotFoundException.class);
    }
}
