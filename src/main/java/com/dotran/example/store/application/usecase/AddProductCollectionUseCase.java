package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface AddProductCollectionUseCase {

    StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd);
}
