package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.store.CreateStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface CreateStoreUseCase {

    StoreDetailDto create(CreateStoreCmd cmd);
}
