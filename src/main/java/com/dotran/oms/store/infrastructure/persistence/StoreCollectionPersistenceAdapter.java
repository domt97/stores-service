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
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.infrastructure.mapper.StoreCollectionPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreCollectionRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public StoreCollection addProducts(StoreCollection storeCollection, List<ProductId> productIds) {
        StoreCollectionEntity storeCollectionEntity = this.getEntity(storeCollection);

        List<ProductCollectionEntity> newProductCollections =
                this.createProductCollectionList(storeCollectionEntity, productIds);

        storeCollectionEntity.getProducts().addAll(newProductCollections);
        storeCollectionEntity.setUpdatedAt(storeCollection.getUpdatedAt());

        StoreCollectionEntity updatedCollection = storeCollectionRepository.saveAndFlush(storeCollectionEntity);

        return storeCollectionPersistenceMapper.toStoreCollection(updatedCollection);
    }

    @Override
    public StoreCollection removeProducts(StoreCollection storeCollection, List<ProductId> toRemoveProductIds) {
        StoreCollectionEntity storeCollectionEntity = this.getEntity(storeCollection);

        if (CollectionUtils.isEmpty(storeCollectionEntity.getProducts())
                || CollectionUtils.isEmpty(toRemoveProductIds)) {
            return storeCollectionPersistenceMapper.toStoreCollection(storeCollectionEntity);
        }

        Set<UUID> toRemoveProductIdsSet = toRemoveProductIds.stream()
                .map(BaseId::getValue)
                .collect(Collectors.toSet());

        storeCollectionEntity.getProducts().removeIf(productCollectionEntity ->
                toRemoveProductIdsSet.contains(productCollectionEntity.getProductId())
        );
        storeCollectionEntity.setUpdatedAt(storeCollection.getUpdatedAt());

        StoreCollectionEntity updatedStoreCollectionEntity = storeCollectionRepository.saveAndFlush(storeCollectionEntity);

        return storeCollectionPersistenceMapper.toStoreCollection(updatedStoreCollectionEntity);
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

    // Defence in depth: the service has already loaded the collection scoped by store.
    private StoreCollectionEntity getEntity(StoreCollection storeCollection) {
        return storeCollectionRepository
                .findByIdAndStoreId(storeCollection.getId().getValue(), storeCollection.getStoreId().getValue())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND));
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
