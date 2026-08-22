package com.dotran.example.store.application.usecase.storeproduct;

import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.common.dto.DomainPageRequest;

import java.util.List;
import java.util.UUID;

public interface GetListStoreProductUseCase {

    List<StoreProductReviewDto> getListProductByStoreId(UUID tenantId, UUID storeId, DomainPageRequest pageRequest);
}
