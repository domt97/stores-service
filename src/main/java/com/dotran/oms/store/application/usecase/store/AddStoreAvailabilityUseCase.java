package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;

public interface AddStoreAvailabilityUseCase {

    StoreAvailabilityDto add(AddStoreAvailabilityCmd cmd);
}
