package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.store.application.command.collection.RemoveProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;

public interface RemoveProductFromCollectionUseCase {

    StoreCollectionDto removeProducts(RemoveProductCollectionCmd cmd);
}
