package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;

public interface CreateStoreCollectionUseCase {

    StoreCollectionDto create(CreateStoreCollectionCmd cmd);
}
