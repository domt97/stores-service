package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.GetStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;

public interface GetStoreUseCase {

    StoreDetailDto getStoreByTenantIdAndStoreId(GetStoreCmd getStoreCmd);
}
