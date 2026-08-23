package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.domain.valueobject.BaseId;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.common.utils.CollectionUtils;
import com.dotran.example.store.domain.model.StoreCollection;
import com.dotran.example.store.infrastructure.mapper.StoreCollectionPersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreCollectionEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreCollectionRepository;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
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
    private final SpringDataStoreProductRepository storeProductRepository;
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
        StoreCollectionEntity storeCollectionEntity = storeCollectionRepository
                .findById(storeCollection.getId().getValue())
                .orElseThrow(NotFoundException::new);

        List<ProductCollectionEntity> newProductCollections =
                this.createProductCollectionList(storeCollectionEntity, productIds);

        if (CollectionUtils.isEmpty(storeCollectionEntity.getProducts())) {
            storeCollectionEntity.setProducts(new ArrayList<>());
        }

        storeCollectionEntity.getProducts().addAll(newProductCollections);

        StoreCollectionEntity updatedCollection = storeCollectionRepository.saveAndFlush(storeCollectionEntity);

        return storeCollectionPersistenceMapper.toStoreCollection(updatedCollection);
    }

    @Override
    public StoreCollection removeProducts(StoreCollection storeCollection, List<ProductId> toRemoveProductIds) {
        StoreCollectionEntity storeCollectionEntity = storeCollectionRepository
                .findById(storeCollection.getId().getValue())
                .orElseThrow(NotFoundException::new);

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

        StoreCollectionEntity updatedStoreCollectionEntity = storeCollectionRepository.saveAndFlush(storeCollectionEntity);

        return storeCollectionPersistenceMapper.toStoreCollection(updatedStoreCollectionEntity);
    }

    @Override
    public Optional<StoreCollection> getById(StoreCollectionId storeCollectionId) {
        return storeCollectionRepository
                .findById(storeCollectionId.getValue())
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

    private List<ProductCollectionEntity> createProductCollectionList(StoreCollectionEntity collectionEntity, List<ProductId> productIds) {
        List<StoreProductEntity> productEntityList = storeProductRepository
                .findAllByIdIn(productIds
                        .stream()
                        .map(BaseId::getValue).toList());

        List<ProductCollectionEntity> productCollectionEntities = new ArrayList<>();
        for (StoreProductEntity productEntity : productEntityList) {
            ProductCollectionEntity productCollectionEntity = new ProductCollectionEntity();
            productCollectionEntity.setCollection(collectionEntity);
            productCollectionEntity.setProductId(productEntity.getId());
            productCollectionEntities.add(productCollectionEntity);
        }

        return productCollectionEntities;
    }
}
