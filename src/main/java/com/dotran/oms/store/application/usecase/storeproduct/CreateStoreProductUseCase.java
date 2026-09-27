package com.dotran.oms.store.application.usecase.storeproduct;

import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;

public interface CreateStoreProductUseCase {

    StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd);
}
