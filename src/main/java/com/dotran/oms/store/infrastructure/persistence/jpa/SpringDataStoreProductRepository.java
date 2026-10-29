package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /**
     * Guarded version bump (update-product). Also stamps updated_at (bound from Java, the same clock as Hibernate's
     * timestamps), so the row changes even when only SKUs changed.
     * Not clearAutomatically: the loaded entity must stay managed (the change set is applied to it afterwards).
     */
    @Modifying
    @Query(value = """
        UPDATE store_products
        SET version    = version + 1,
            updated_at = :updatedAt
        WHERE id       = :productId
          AND store_id = :storeId
          AND version  = :version
        """, nativeQuery = true)
    int bumpVersion(@Param("productId") UUID productId,
                    @Param("storeId") UUID storeId,
                    @Param("version") long version,
                    @Param("updatedAt") Instant updatedAt);

    @Query(value = "SELECT version FROM store_products WHERE id = :productId AND store_id = :storeId", nativeQuery = true)
    Optional<Long> findVersion(@Param("productId") UUID productId, @Param("storeId") UUID storeId);

    /** SKU codes used in the store, removed SKUs included (index uk_product_skus_store_sku). */
    @Query("select s.sku from ProductSkuEntity s where s.storeId = :storeId and s.sku in :codes")
    Set<String> findSkuCodesInStore(@Param("storeId") UUID storeId, @Param("codes") Collection<String> codes);

    @Query("select s.sku from ProductSkuEntity s where s.storeId = :storeId and s.product.id <> :productId and s.sku in :codes")
    Set<String> findSkuCodesOfOtherProducts(@Param("storeId") UUID storeId,
                                            @Param("productId") UUID productId,
                                            @Param("codes") Collection<String> codes);
}
