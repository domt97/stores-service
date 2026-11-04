package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.ProductChangeSet;
import com.dotran.oms.store.domain.model.StoreProduct;

import java.time.Instant;
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

    /**
     * Guarded version bump: {@code version + 1} only if the stored version is {@code expectedVersion}. Locks the product
     * row until commit. Returns false if another update won. The only writer of the version.
     */
    boolean bumpVersion(StoreId storeId, ProductId productId, long expectedVersion, Instant updatedAt);

    /** The stored version, read directly (not from the persistence context). */
    long currentVersion(StoreId storeId, ProductId productId);

    /** SKU codes among {@code codes} already used in the store, removed SKUs included. Empty input → no query. */
    Set<SKU> findSkuCodesInStore(StoreId storeId, Collection<SKU> codes);

    /** As {@link #findSkuCodesInStore}, but leaving out the product's own SKUs. */
    Set<SKU> findSkuCodesOfOtherProducts(StoreId storeId, ProductId productId, Collection<SKU> codes);

    /**
     * Applies a change set from {@link StoreProduct#replace} to the stored product and flushes; returns the product as
     * stored (new SKU ids assigned) with {@code newVersion}. A SKU code that collides in the database → SkuCodeInUseException.
     */
    StoreProduct applyChanges(StoreProduct product, ProductChangeSet changes, long newVersion);
}
