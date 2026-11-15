package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreAvailabilityRepository extends JpaRepository<StoreAvailabilityEntity, UUID> {

    Optional<StoreAvailabilityEntity> findByIdAndStoreId(UUID id, UUID storeId);

    /** Windows covering {@code now}; compared by the database with an Instant bound like the writes (index idx_store_availability_lookup). */
    @Query("select a from StoreAvailabilityEntity a where a.storeId = :storeId and a.cancelled = false "
            + "and a.startTime <= :now and (a.endTime is null or a.endTime > :now)")
    List<StoreAvailabilityEntity> findActiveCovering(@Param("storeId") UUID storeId, @Param("now") Instant now);
}
