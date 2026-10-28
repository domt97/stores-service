package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.StoreProduct;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StoreProductRepository {

    StoreProduct create(StoreProduct storeProduct);

    Optional<StoreProduct> getByStoreIdAndProductId(StoreId storeId, ProductId productId);

    Set<ProductId> findExistingProductIds(StoreId storeId, Collection<ProductId> productIds);

    /**
     * Products of the store, optionally without those already in {@code excludeCollectionId}.
     * Always ordered with {@code id} as the last key so offset paging is stable.
     */
    PagedResult<StoreProduct> searchProducts(StoreId storeId, PriceRange priceRange, StoreCollectionId excludeCollectionId,
                                             DomainPageRequest pageRequest);

    /** Products of the store that are in the collection, ordered by name then id. */
    PagedResult<StoreProduct> findInCollection(StoreId storeId, StoreCollectionId collectionId, DomainPageRequest pageRequest);

    List<StoreProduct> saveAll(List<StoreProduct> storeProductList);
}
