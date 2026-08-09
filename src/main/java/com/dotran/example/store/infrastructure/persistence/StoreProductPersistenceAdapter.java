package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StoreProductPersistenceAdapter implements StoreProductRepository {

    private final SpringDataStoreProductRepository repository;
    private final StoreProductPersistenceMapper mapper;

    @Override
    public StoreProduct create(StoreProduct storeProduct) {
        StoreProductEntity storeProductEntity = mapper.fromStoreProduct(storeProduct);
        storeProductEntity.setSkus(mapper.fromProductSkus(storeProduct.getSkus(), storeProductEntity));
        storeProductEntity.setImages(mapper.fromProductImages(storeProduct.getImages(), storeProductEntity));

        StoreProductEntity savedEntity = repository.saveAndFlush(storeProductEntity);

        return mapper.fromEntity(savedEntity);
    }
}
