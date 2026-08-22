package com.dotran.example.store.application.repository;

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

    PagedResult<StoreProduct> getListByStoreId(StoreId storeId, DomainPageRequest pageRequest);

    List<StoreProduct> getProductsByListOfProductIds(List<ProductId> productIds);
}
