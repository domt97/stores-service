package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface CreateStoreCollectionUseCase {

    StoreCollectionDto create(CreateStoreCollectionCmd cmd);
}
