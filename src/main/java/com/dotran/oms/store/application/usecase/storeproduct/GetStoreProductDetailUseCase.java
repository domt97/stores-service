package com.dotran.oms.store.application.usecase.storeproduct;

import com.dotran.oms.store.application.dto.StoreProductDetailDto;

import java.util.UUID;

public interface GetStoreProductDetailUseCase {

    StoreProductDetailDto getProductById(UUID merchantId, UUID storeId, UUID productId);
}
