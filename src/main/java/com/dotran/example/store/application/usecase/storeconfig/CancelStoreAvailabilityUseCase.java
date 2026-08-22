package com.dotran.example.store.application.usecase.storeconfig;

import java.util.UUID;

public interface CancelStoreAvailabilityUseCase {

    void cancel(UUID storeAvailabilityId, UUID storeId);
}
