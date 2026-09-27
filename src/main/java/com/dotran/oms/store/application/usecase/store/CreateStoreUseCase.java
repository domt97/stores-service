package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.CreateStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

public interface CreateStoreUseCase {

    StoreDetailDto create(CreateStoreCmd cmd);
}
