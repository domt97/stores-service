package com.dotran.example.store.application.usecase.store;

import com.dotran.example.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;

public interface AddStoreAvailabilityUseCase {

    StoreAvailabilityDto add(AddStoreAvailabilityCmd cmd);
}
