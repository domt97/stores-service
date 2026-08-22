package com.dotran.example.store.application.usecase;

import com.dotran.example.store.application.command.storeconfig.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;

public interface AddStoreAvailabilityUseCase {

    StoreAvailabilityDto add(AddStoreAvailabilityCmd cmd);
}
