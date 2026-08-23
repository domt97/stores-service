package com.dotran.example.store.application.usecase.collection;

import com.dotran.example.store.application.command.collection.GetListCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;

import java.util.List;

public interface GetListStoreCollectionUseCase {

    List<StoreCollectionDto> getListCollectionByStoreId(GetListCollectionCmd cmd);
}
