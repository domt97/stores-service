package com.dotran.oms.store.application.usecase.storeconfig;

import com.dotran.oms.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

import java.util.List;
import java.util.UUID;

public interface SettingStoreBusinessHourUseCase {

    StoreDetailDto setupBusinessHour(UUID tenantId, UUID storeId, List<UpdateBusinessHourCmd> updateBusinessHourCmds);
}
