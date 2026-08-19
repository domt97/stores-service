package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreProductRepository extends JpaRepository<StoreProductEntity, UUID> {

    Optional<StoreProductEntity> findByIdAndStoreId(UUID id, UUID storeId);

    List<StoreProductEntity> findByStoreId(UUID storeId, Pageable pageable);

    List<StoreProductEntity> findAllByIdIn(Collection<UUID> ids);
}
