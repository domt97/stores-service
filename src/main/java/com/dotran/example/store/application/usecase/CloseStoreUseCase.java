package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.CloseStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface CloseStoreUseCase {

    StoreDetailDto close(CloseStoreCmd cmd);
}
