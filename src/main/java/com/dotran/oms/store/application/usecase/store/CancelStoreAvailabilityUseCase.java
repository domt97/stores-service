package com.dotran.oms.store.application.usecase.store;

import com.dotran.oms.store.application.command.store.CancelStoreAvailabilityCmd;

public interface CancelStoreAvailabilityUseCase {

    void cancel(CancelStoreAvailabilityCmd cmd);
}
