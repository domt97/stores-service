package com.dotran.example.store.application.usecase.collection;

import com.dotran.example.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface CreateStoreCollectionUseCase {

    StoreCollectionDto create(CreateStoreCollectionCmd cmd);
}
