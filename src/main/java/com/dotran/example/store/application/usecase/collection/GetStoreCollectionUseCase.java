package com.dotran.example.store.application.usecase.collection;

import com.dotran.example.store.application.command.collection.GetCollectionDetailCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface GetStoreCollectionUseCase {

    StoreCollectionDto getCollectionById(GetCollectionDetailCmd cmd);
}
