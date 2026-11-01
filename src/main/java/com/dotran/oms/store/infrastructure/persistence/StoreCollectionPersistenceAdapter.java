package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.id.BaseId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.util.CollectionUtils;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.exception.CollectionChangedConcurrentlyException;
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.infrastructure.mapper.StoreCollectionPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
@RequiredArgsConstructor
public class StoreCollectionPersistenceAdapter implements StoreCollectionRepository {

    private final SpringDataStoreCollectionRepository storeCollectionRepository;
    private final StoreCollectionPersistenceMapper storeCollectionPersistenceMapper;

    @Override
    public StoreCollection create(StoreCollection storeCollection) {
        StoreCollectionEntity storeCollectionEntity = storeCollectionPersistenceMapper
                .toBaseEntity(storeCollection);

        List<ProductCollectionEntity> newProductCollections =
                this.createProductCollectionList(storeCollectionEntity, storeCollection.getProductIds());
        storeCollectionEntity.setProducts(newProductCollections);
        storeCollectionEntity.setUpdatedAt(storeCollection.getUpdatedAt());

        StoreCollectionEntity savedStoreCollection = storeCollectionRepository.saveAndFlush(storeCollectionEntity);

        return storeCollectionPersistenceMapper.toStoreCollection(savedStoreCollection);
    }

    /**
     * Membership changes are single native statements (no load-modify-save of the entity graph): concurrent requests are
     * resolved by the unique index (add) and row locks (remove) instead of failing. See BR-CAT-15.
     */
    @Override
    public StoreCollection addProducts(StoreId store, StoreCollectionId collection, List<ProductId> productIds, Instant now) {
        UUID collectionId = collection.getValue();
        UUID storeId = store.getValue();
        if (!CollectionUtils.isEmpty(productIds)) {
            try {
                int inserted = storeCollectionRepository.insertProductsSkippingExisting(collectionId, storeId, uuids(productIds), now);
                if (inserted > 0) {
                    storeCollectionRepository.touchUpdatedAt(collectionId, storeId, now);
                }
            } catch (PessimisticLockingFailureException ex) {
                throw new CollectionChangedConcurrentlyException();
            }
        }
        return reRead(collectionId, storeId);
    }

    @Override
    public StoreCollection removeProducts(StoreId store, StoreCollectionId collection, List<ProductId> productIds, Instant now) {
        UUID collectionId = collection.getValue();
        UUID storeId = store.getValue();
        if (!CollectionUtils.isEmpty(productIds)) {
            try {
                int deleted = storeCollectionRepository.deleteProducts(collectionId, storeId, uuids(productIds));
                if (deleted > 0) {
                    storeCollectionRepository.touchUpdatedAt(collectionId, storeId, now);
                }
            } catch (PessimisticLockingFailureException ex) {
                throw new CollectionChangedConcurrentlyException();
            }
        }
        return reRead(collectionId, storeId);
    }

    private StoreCollection reRead(UUID collectionId, UUID storeId) {
        return storeCollectionRepository.findByIdAndStoreId(collectionId, storeId)
                .map(storeCollectionPersistenceMapper::toStoreCollection)
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND));
    }

    private static List<UUID> uuids(List<ProductId> productIds) {
        return productIds.stream().map(BaseId::getValue).toList();
    }

    @Override
    public Optional<StoreCollection> getByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId) {
        return storeCollectionRepository
                .findByIdAndStoreId(storeCollectionId.getValue(), storeId.getValue())
                .map(storeCollectionPersistenceMapper::toStoreCollection);
    }

    @Override
    public List<StoreCollection> getListCollectionByStoreId(StoreId storeId) {
        List<StoreCollectionEntity> storeCollectionEntityList =
                storeCollectionRepository.findAllByStoreId(storeId.getValue());

        return storeCollectionEntityList.stream()
                .map(storeCollectionPersistenceMapper::toStoreCollection)
                .toList();
    }

    @Override
    public boolean existsByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId) {
        return storeCollectionRepository.existsByIdAndStoreId(storeCollectionId.getValue(), storeId.getValue());
    }

    // Product ids are validated against the collection's store by the use case; the FK backs it.
    private List<ProductCollectionEntity> createProductCollectionList(StoreCollectionEntity collectionEntity, List<ProductId> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return new ArrayList<>();
        }

        List<ProductCollectionEntity> productCollectionEntities = new ArrayList<>();
        for (ProductId productId : productIds) {
            ProductCollectionEntity productCollectionEntity = new ProductCollectionEntity();
            productCollectionEntity.setCollection(collectionEntity);
            productCollectionEntity.setProductId(productId.getValue());
            productCollectionEntities.add(productCollectionEntity);
        }

        return productCollectionEntities;
    }
}
