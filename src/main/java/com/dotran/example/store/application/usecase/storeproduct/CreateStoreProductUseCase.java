package com.dotran.example.store.application.usecase.storeproduct;

import com.dotran.example.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;

public interface CreateStoreProductUseCase {

    StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd);
}
