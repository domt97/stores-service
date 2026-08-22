package com.dotran.example.store.application.usecase.storeproduct;

import com.dotran.example.store.application.dto.StoreProductDetailDto;

import java.util.UUID;

public interface GetStoreProductDetailUseCase {

    StoreProductDetailDto getProductById(UUID tenantId, UUID storeId, UUID productId);
}
