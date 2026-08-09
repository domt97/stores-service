package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreAvailabilityRepository extends JpaRepository<StoreAvailabilityEntity, UUID> {

    Optional<StoreAvailabilityEntity> findByIdAndStoreId(UUID id, UUID storeId);
}
