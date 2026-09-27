package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.ReopenStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

public interface ReopenStoreUseCase {

    StoreDetailDto reopen(ReopenStoreCmd cmd);
}
