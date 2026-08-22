package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreProductRepository extends JpaRepository<StoreProductEntity, UUID>, JpaSpecificationExecutor<StoreProductEntity> {

    Optional<StoreProductEntity> findByIdAndStoreId(UUID id, UUID storeId);

    Page<StoreProductEntity> findByStoreId(UUID storeId, Pageable pageable);

    List<StoreProductEntity> findAllByIdIn(Collection<UUID> ids);
}
