package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.store.application.command.collection.GetListCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;

import java.util.List;

public interface GetListStoreCollectionUseCase {

    List<StoreCollectionDto> getListCollectionByStoreId(GetListCollectionCmd cmd);
}
