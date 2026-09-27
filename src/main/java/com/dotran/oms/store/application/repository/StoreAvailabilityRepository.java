package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.model.StoreAvailability;

import java.util.Optional;

public interface StoreAvailabilityRepository {

    StoreAvailability save(StoreAvailability storeAvailability);

    Optional<StoreAvailability> findByIdAndStoreId(StoreAvailabilityId storeAvailabilityId, StoreId storeId);
}
