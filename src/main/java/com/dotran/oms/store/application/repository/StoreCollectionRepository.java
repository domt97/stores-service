package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.StoreCollection;

import java.util.List;
import java.util.Optional;

public interface StoreCollectionRepository {

    StoreCollection create(StoreCollection storeCollection);

    StoreCollection addProducts(StoreCollection storeCollection, List<ProductId> productIds);

    StoreCollection removeProducts(StoreCollection storeCollection, List<ProductId> toRemoveProductIds);

    Optional<StoreCollection> getByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId);

    boolean existsByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId);

    List<StoreCollection> getListCollectionByStoreId(StoreId storeId);
}
