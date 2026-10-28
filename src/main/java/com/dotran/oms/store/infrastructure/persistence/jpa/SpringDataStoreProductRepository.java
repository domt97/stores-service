package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpringDataStoreProductRepository extends JpaRepository<StoreProductEntity, UUID>, JpaSpecificationExecutor<StoreProductEntity> {

    Optional<StoreProductEntity> findByIdAndStoreId(UUID id, UUID storeId);

    Page<StoreProductEntity> findByStoreId(UUID storeId, Pageable pageable);

    @Query(value = "select p from StoreProductEntity p where p.storeId = :storeId and exists ("
            + "select 1 from ProductCollectionEntity pc where pc.collection.id = :collectionId and pc.productId = p.id) "
            + "order by p.name asc, p.id asc",
            countQuery = "select count(p) from StoreProductEntity p where p.storeId = :storeId and exists ("
                    + "select 1 from ProductCollectionEntity pc where pc.collection.id = :collectionId and pc.productId = p.id)")
    Page<StoreProductEntity> findInCollection(@Param("storeId") UUID storeId, @Param("collectionId") UUID collectionId, Pageable pageable);

    @Query("select p.id from StoreProductEntity p where p.storeId = :storeId and p.id in :ids")
    Set<UUID> findIdsByStoreIdAndIdIn(@Param("storeId") UUID storeId, @Param("ids") Collection<UUID> ids);
}
