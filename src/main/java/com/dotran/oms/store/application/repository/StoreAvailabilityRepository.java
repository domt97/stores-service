package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.model.StoreAvailability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoreAvailabilityRepository {

    StoreAvailability save(StoreAvailability storeAvailability);

    /** Windows of the store covering {@code now}: not cancelled, started at or before it, not ended (end null or after). */
    List<StoreAvailability> findActiveCovering(StoreId storeId, Instant now);

    Optional<StoreAvailability> findByIdAndStoreId(StoreAvailabilityId storeAvailabilityId, StoreId storeId);
}
