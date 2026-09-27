package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.CloseStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

public interface CloseStoreUseCase {

    StoreDetailDto close(CloseStoreCmd cmd);
}
