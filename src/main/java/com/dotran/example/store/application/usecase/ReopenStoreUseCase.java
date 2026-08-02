package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.ReopenStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface ReopenStoreUseCase {

    StoreDetailDto reopen(ReopenStoreCmd cmd);
}
