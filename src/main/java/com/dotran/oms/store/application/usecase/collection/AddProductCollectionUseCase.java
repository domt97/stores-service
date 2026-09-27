package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;

public interface AddProductCollectionUseCase {

    StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd);
}
