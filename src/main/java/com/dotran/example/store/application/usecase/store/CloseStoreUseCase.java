package com.dotran.example.store.application.usecase.store;

import com.dotran.example.store.application.command.store.CloseStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface CloseStoreUseCase {

    StoreDetailDto close(CloseStoreCmd cmd);
}
