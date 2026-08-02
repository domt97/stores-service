package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.GetStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;

public interface GetStoreUseCase {

    StoreDetailDto getStoreByTenantIdAndStoreId(GetStoreCmd getStoreCmd);
}
