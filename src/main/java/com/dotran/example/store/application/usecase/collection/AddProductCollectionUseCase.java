package com.dotran.example.store.application.usecase.collection;

import com.dotran.example.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface AddProductCollectionUseCase {

    StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd);
}
