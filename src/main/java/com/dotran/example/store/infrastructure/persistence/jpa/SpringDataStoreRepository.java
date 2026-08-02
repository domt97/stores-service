package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreRepository extends JpaRepository<StoreEntity, Long> {

    Optional<StoreEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Modifying
    @Query("UPDATE StoreEntity s SET s.status = :status WHERE s.tenantId = :tenantId and s.id = :storeId")
    StoreEntity updateStoreStatus(UUID tenantId, UUID storeId);
}
