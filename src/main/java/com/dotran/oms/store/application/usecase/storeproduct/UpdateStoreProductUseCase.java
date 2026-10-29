package com.dotran.oms.store.application.usecase.storeproduct;

import com.dotran.oms.store.application.command.storeproduct.UpdateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;

public interface UpdateStoreProductUseCase {

    StoreProductDetailDto updateProduct(UpdateStoreProductCmd cmd);
}
