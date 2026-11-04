package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.StoreEntity;
import com.dotran.oms.store.domain.enums.StoreStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreRepository extends JpaRepository<StoreEntity, UUID> {

    @EntityGraph(attributePaths = {"address", "config", "businessHours"})
    Optional<StoreEntity> findByMerchantIdAndId(UUID merchantId, UUID id);

    @Modifying
    @Query("UPDATE StoreEntity s SET s.status = :status WHERE s.merchantId = :merchantId and s.id = :storeId")
    int updateStoreStatus(@Param("merchantId") UUID merchantId, @Param("storeId") UUID storeId, @Param("status") StoreStatus status);

    boolean existsByMerchantIdAndId(UUID merchantId, UUID id);
}
