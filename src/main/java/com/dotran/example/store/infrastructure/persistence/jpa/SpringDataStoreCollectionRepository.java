package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.StoreCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataStoreCollectionRepository extends JpaRepository<StoreCollectionEntity, UUID> {

    List<StoreCollectionEntity> findAllByStoreId(UUID storeId);
}
