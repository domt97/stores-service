package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.AddProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

public interface AddProductCollectionUseCase {

    StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd);
}
