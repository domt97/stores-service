package com.dotran.example.store.application.repository;

import com.dotran.example.store.common.domain.valueobject.StoreAvailabilityId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.domain.model.StoreAvailability;

import java.util.Optional;

public interface StoreAvailabilityRepository {

    StoreAvailability save(StoreAvailability storeAvailability);

    Optional<StoreAvailability> findByIdAndStoreId(StoreAvailabilityId storeAvailabilityId, StoreId storeId);
}
