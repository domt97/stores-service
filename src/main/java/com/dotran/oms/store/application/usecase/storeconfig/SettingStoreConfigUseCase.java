package com.dotran.oms.store.application.usecase.storeconfig;

import com.dotran.oms.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

import java.util.UUID;

public interface SettingStoreConfigUseCase {

    StoreDetailDto setupStoreConfig(UUID tenantId, UUID storeId, UpdateStoreConfigCmd cmd);
}
