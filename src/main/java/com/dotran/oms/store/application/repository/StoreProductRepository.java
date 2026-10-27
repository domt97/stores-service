package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.domain.model.StoreProduct;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StoreProductRepository {

    StoreProduct create(StoreProduct storeProduct);

    Optional<StoreProduct> getByStoreIdAndProductId(StoreId storeId, ProductId productId);

    Set<ProductId> findExistingProductIds(StoreId storeId, Collection<ProductId> productIds);

    PagedResult<StoreProduct> searchProducts(StoreId storeId, PriceRange priceRange, DomainPageRequest pageRequest);

    List<StoreProduct> saveAll(List<StoreProduct> storeProductList);
}
