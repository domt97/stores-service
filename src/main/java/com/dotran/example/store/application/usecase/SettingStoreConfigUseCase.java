package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.UpdateBusinessHourCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

import java.util.List;
import java.util.UUID;

public interface SettingStoreConfigUseCase {

    StoreDetailDto setupBusinessHour(UUID tenantId, UUID storeId, List<UpdateBusinessHourCmd> updateBusinessHourCmds);
}
