package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<StoreProduct> getByStoreIdAndProductId(StoreId storeId, ProductId productId) {
        return repository.findByIdAndStoreId(productId.getValue(), storeId.getValue())
                .map(mapper::fromEntity);
    }

    @Override
    public List<StoreProduct> getListByStoreId(StoreId storeId, DomainPageRequest pageRequest) {
        return repository.findByStoreId(storeId.getValue(), pageRequest.toPageRequest())
                .stream()
                .map(mapper::fromEntity)
                .toList();
    }
}
