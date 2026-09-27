package com.dotran.oms.store.application.usecase.store;

import java.util.UUID;

public interface CancelStoreAvailabilityUseCase {

    void cancel(UUID storeAvailabilityId, UUID storeId);
}
