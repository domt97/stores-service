package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.GetCollectionDetailCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface GetStoreCollectionUseCase {

    StoreCollectionDto getCollectionById(GetCollectionDetailCmd cmd);
}
