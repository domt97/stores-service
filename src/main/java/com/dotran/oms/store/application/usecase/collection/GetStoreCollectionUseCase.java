package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.store.application.command.collection.GetCollectionDetailCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;

public interface GetStoreCollectionUseCase {

    StoreCollectionDto getCollectionById(GetCollectionDetailCmd cmd);
}
