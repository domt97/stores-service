package com.dotran.example.store.application.usecase.store;

import com.dotran.example.store.application.command.store.ReopenStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface ReopenStoreUseCase {

    StoreDetailDto reopen(ReopenStoreCmd cmd);
}
