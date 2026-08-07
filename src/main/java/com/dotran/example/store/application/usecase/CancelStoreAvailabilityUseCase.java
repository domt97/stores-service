package com.dotran.example.store.application.usecase;

import java.util.UUID;

public interface CancelStoreAvailabilityUseCase {

    void cancel(UUID storeAvailabilityId, UUID storeId);
}
