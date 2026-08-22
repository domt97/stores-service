package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.collection.RemoveProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface RemoveProductFromCollectionUseCase {

    StoreCollectionDto removeProducts(RemoveProductCollectionCmd cmd);
}
