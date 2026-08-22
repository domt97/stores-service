package com.dotran.example.store.application.repository;

import com.dotran.example.store.common.domain.valueobject.PriceRange;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.dto.PagedResult;
import com.dotran.example.store.domain.model.StoreProduct;

import java.util.List;
import java.util.Optional;

public interface StoreProductRepository {

    StoreProduct create(StoreProduct storeProduct);

    Optional<StoreProduct> getByStoreIdAndProductId(StoreId storeId, ProductId productId);

    List<StoreProduct> getProductsByListOfProductIds(List<ProductId> productIds);

    PagedResult<StoreProduct> searchProducts(StoreId storeId, PriceRange priceRange, DomainPageRequest pageRequest);
}
