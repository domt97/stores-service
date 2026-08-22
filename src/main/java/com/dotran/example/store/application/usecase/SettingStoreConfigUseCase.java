package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

import java.util.UUID;

public interface SettingStoreConfigUseCase {

    StoreDetailDto setupStoreConfig(UUID tenantId, UUID storeId, UpdateStoreConfigCmd cmd);
}
