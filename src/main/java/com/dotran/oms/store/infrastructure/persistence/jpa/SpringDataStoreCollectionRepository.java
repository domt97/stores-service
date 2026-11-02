package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.StoreCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStoreCollectionRepository extends JpaRepository<StoreCollectionEntity, UUID> {

    List<StoreCollectionEntity> findAllByStoreId(UUID storeId);

    Optional<StoreCollectionEntity> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByIdAndStoreId(UUID id, UUID storeId);

    /**
     * Adds the store's products to the collection, skipping those already in it (BR-CAT-15). Scoped by store in SQL.
     * Rows are inserted in one global order (Postgres uuid order) so overlapping concurrent requests cannot deadlock on
     * the unique index; ON CONFLICT makes the concurrent duplicate a no-op instead of an error. Never call it with an
     * empty list (native IN () is a syntax error). Clears the persistence context: re-read the collection afterwards.
     *
     * @return rows inserted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        INSERT INTO product_collections (collection_id, product_id, created_at)
        SELECT c.id, p.id, :createdAt
        FROM store_collections c
        JOIN store_products p ON p.store_id = c.store_id
        WHERE c.id = :collectionId AND c.store_id = :storeId AND p.id IN (:productIds)
        ORDER BY p.id
        ON CONFLICT (collection_id, product_id) DO NOTHING
        """, nativeQuery = true)
    int insertProductsSkippingExisting(@Param("collectionId") UUID collectionId,
                                       @Param("storeId") UUID storeId,
                                       @Param("productIds") Collection<UUID> productIds,
                                       @Param("createdAt") Instant createdAt);

    /**
     * Removes the products from the collection; products not in it are skipped (BR-CAT-15). Rows are locked in one
     * global order before the delete so overlapping concurrent requests cannot deadlock. Never call it with an empty list.
     *
     * @return rows deleted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        DELETE FROM product_collections
        WHERE id IN (
            SELECT pc.id
            FROM product_collections pc
            JOIN store_collections c ON c.id = pc.collection_id
            WHERE pc.collection_id = :collectionId AND c.store_id = :storeId AND pc.product_id IN (:productIds)
            ORDER BY pc.product_id
            FOR UPDATE OF pc)
        """, nativeQuery = true)
    int deleteProducts(@Param("collectionId") UUID collectionId,
                       @Param("storeId") UUID storeId,
                       @Param("productIds") Collection<UUID> productIds);

    /** Moves updated_at forward (never back: a later commit may carry an earlier timestamp). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StoreCollectionEntity c set c.updatedAt = greatest(c.updatedAt, :updatedAt) where c.id = :id and c.storeId = :storeId")
    int touchUpdatedAt(@Param("id") UUID id, @Param("storeId") UUID storeId, @Param("updatedAt") Instant updatedAt);
}
