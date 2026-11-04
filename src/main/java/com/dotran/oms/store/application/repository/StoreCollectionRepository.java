package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.StoreCollection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoreCollectionRepository {

    StoreCollection create(StoreCollection storeCollection);

    /**
     * Adds the products (of the collection's store) to the collection; those already in it are skipped (BR-CAT-15).
     * updated_at moves to {@code now} only if a row was added. Returns the collection as stored after the change.
     */
    StoreCollection addProducts(StoreId storeId, StoreCollectionId collectionId, List<ProductId> productIds, Instant now);

    /**
     * Removes the products from the collection; those not in it are skipped (BR-CAT-15). updated_at moves to {@code now}
     * only if a row was removed. Returns the collection as stored after the change.
     */
    StoreCollection removeProducts(StoreId storeId, StoreCollectionId collectionId, List<ProductId> productIds, Instant now);

    Optional<StoreCollection> getByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId);

    boolean existsByStoreIdAndId(StoreId storeId, StoreCollectionId storeCollectionId);

    List<StoreCollection> getListCollectionByStoreId(StoreId storeId);
}
