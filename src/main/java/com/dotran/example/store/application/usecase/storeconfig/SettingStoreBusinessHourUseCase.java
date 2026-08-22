package com.dotran.example.store.application.usecase.storeconfig;

import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

import java.util.List;
import java.util.UUID;

public interface SettingStoreBusinessHourUseCase {

    StoreDetailDto setupBusinessHour(UUID tenantId, UUID storeId, List<UpdateBusinessHourCmd> updateBusinessHourCmds);
}
