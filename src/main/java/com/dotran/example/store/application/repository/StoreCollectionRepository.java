package com.dotran.example.store.application.repository;

import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.domain.model.StoreCollection;

import java.util.List;
import java.util.Optional;

public interface StoreCollectionRepository {

    StoreCollection create(StoreCollection storeCollection);

    StoreCollection addProducts(StoreCollection storeCollection, List<ProductId> productIds);

    StoreCollection removeProducts(StoreCollection storeCollection, List<ProductId> toRemoveProductIds);

    Optional<StoreCollection> getById(StoreCollectionId storeCollectionId);
}
