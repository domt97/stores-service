package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreEntity;
import com.dotran.example.store.domain.enums.StoreStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreRepository extends JpaRepository<StoreEntity, UUID> {

    @EntityGraph(attributePaths = {"address", "config", "businessHours"})
    Optional<StoreEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Modifying
    @Query("UPDATE StoreEntity s SET s.status = :status WHERE s.tenantId = :tenantId and s.id = :storeId")
    int updateStoreStatus(@Param("tenantId") UUID tenantId, @Param("storeId") UUID storeId, @Param("status") StoreStatus status);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);
}
