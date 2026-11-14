package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.exception.ProductVersionConflictException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.model.ProductChangeSet;
import com.dotran.oms.store.domain.model.ProductImage;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductImageEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductSkuEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreProductPersistenceAdapter — update-product")
class StoreProductUpdatePersistenceTest {

    @Mock private SpringDataStoreProductRepository repository;
    @Mock private StoreProductPersistenceMapper mapper;

    @InjectMocks
    private StoreProductPersistenceAdapter adapter;

    private final UUID storeId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID skuAId = UUID.randomUUID();
    private final UUID skuBId = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private StoreProductEntity entity() {
        StoreProductEntity entity = new StoreProductEntity();
        entity.setId(productId);
        entity.setStoreId(storeId);
        entity.setName("Milk Tea");
        entity.setSkus(new ArrayList<>(List.of(skuEntity(skuAId, "A"), skuEntity(skuBId, "B"))));
        entity.setImages(new ArrayList<>(List.of(new ProductImageEntity())));
        return entity;
    }

    private ProductSkuEntity skuEntity(UUID id, String code) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setStoreId(storeId);
        sku.setSku(code);
        sku.setPrice(new BigDecimal("100.00"));
        sku.setCurrency("VND");
        sku.setStatus(ProductSkuStatus.ACTIVE);
        return sku;
    }

    private ProductSku domainSku(UUID id, String code, String price, Instant removedAt) {
        return ProductSku.builder().id(id != null ? ProductSkuId.of(id) : null).productId(ProductId.of(productId))
                .sku(SKU.of(code)).price(new BigDecimal(price)).currency("VND").status(ProductSkuStatus.INACTIVE)
                .removedAt(removedAt).build();
    }

    private StoreProduct product(List<ProductImage> images) {
        return StoreProduct.builder().id(ProductId.of(productId)).storeId(StoreId.of(storeId)).name("Bubble Tea")
                .status(ProductStatus.INACTIVE).images(images).skus(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("applies changes in place: same collections, updated and removed SKUs copied, added SKUs sorted by code, version set")
    void appliesInPlace() {
        StoreProductEntity entity = entity();
        List<ProductSkuEntity> skuList = entity.getSkus();
        List<ProductImageEntity> imageList = entity.getImages();
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(mapper.fromProductSku(any(), any())).thenAnswer(inv -> skuEntity(null, ((ProductSku) inv.getArgument(0)).getSku().getValue()));
        StoreProduct product = product(List.of());
        when(mapper.fromEntity(entity)).thenReturn(product);

        ProductChangeSet changes = new ProductChangeSet(true, false,
                List.of(domainSku(null, "Z", "1", null), domainSku(null, "C", "1", null)),
                List.of(domainSku(skuAId, "A", "120", null)),
                List.of(domainSku(skuBId, "B", "100", NOW)));

        StoreProduct result = adapter.applyChanges(product, changes, 8L);

        assertThat(result).isSameAs(product);
        assertThat(entity.getSkus()).isSameAs(skuList);
        assertThat(entity.getImages()).isSameAs(imageList).hasSize(1);
        assertThat(entity.getName()).isEqualTo("Bubble Tea");
        assertThat(entity.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(entity.getVersion()).isEqualTo(8L);
        assertThat(skuList).filteredOn(s -> skuAId.equals(s.getId())).singleElement().satisfies(s -> {
            assertThat(s.getPrice()).isEqualByComparingTo("120");
            assertThat(s.getStatus()).isEqualTo(ProductSkuStatus.INACTIVE);
            assertThat(s.getRemovedAt()).isNull();
        });
        assertThat(skuList).filteredOn(s -> skuBId.equals(s.getId())).singleElement()
                .satisfies(s -> assertThat(s.getRemovedAt()).isEqualTo(NOW));
        assertThat(skuList).extracting(ProductSkuEntity::getSku).containsExactly("A", "B", "C", "Z");
        verify(repository).flush();
    }

    @Test
    @DisplayName("images are replaced (clear + add) only when they changed")
    void imagesReplacedWhenChanged() {
        StoreProductEntity entity = entity();
        List<ProductImageEntity> imageList = entity.getImages();
        ProductImageEntity newImage = new ProductImageEntity();
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        StoreProduct product = product(List.of(ProductImage.builder().imageUrl("https://img/new.jpg").displayOrder(1).build()));
        when(mapper.fromProductImages(product.getImages(), entity)).thenReturn(List.of(newImage));
        when(mapper.fromEntity(entity)).thenReturn(product);

        adapter.applyChanges(product, new ProductChangeSet(false, true, List.of(), List.of(), List.of()), 2L);

        assertThat(entity.getImages()).isSameAs(imageList).containsExactly(newImage);
        assertThat(entity.getName()).isEqualTo("Milk Tea");
    }

    @Test
    @DisplayName("a SKU code collision in the database → SkuCodeInUseException; other integrity errors are rethrown")
    void flushCollision_mapped() {
        StoreProductEntity entity = entity();
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        StoreProduct product = product(List.of());
        ProductChangeSet changes = new ProductChangeSet(true, false, List.of(), List.of(), List.of());

        doThrow(new DataIntegrityViolationException("dup", new SQLException(
                "duplicate key value violates unique constraint \"uk_product_skus_store_sku\""))).when(repository).flush();
        assertThatThrownBy(() -> adapter.applyChanges(product, changes, 2L)).isInstanceOf(SkuCodeInUseException.class);

        DataIntegrityViolationException other = new DataIntegrityViolationException("fk", new SQLException("violates foreign key"));
        doThrow(other).when(repository).flush();
        assertThatThrownBy(() -> adapter.applyChanges(product, changes, 2L)).isSameAs(other);

        doThrow(new CannotAcquireLockException("deadlock")).when(repository).flush();
        assertThatThrownBy(() -> adapter.applyChanges(product, changes, 2L)).isInstanceOf(ProductVersionConflictException.class);
    }

    @Test
    @DisplayName("a collision reported through Hibernate under JDBC batching (ConstraintViolationException → BatchUpdateException) → 409")
    void batchedCollision_mapped() {
        StoreProductEntity entity = entity();
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        java.sql.BatchUpdateException batch = new java.sql.BatchUpdateException("Batch entry 0 was aborted", new int[0]);
        batch.setNextException(new SQLException("ERROR: duplicate key value violates unique constraint \"uk_product_skus_store_sku\""));
        org.hibernate.exception.ConstraintViolationException hibernate =
                new org.hibernate.exception.ConstraintViolationException("could not execute batch", batch, "uk_product_skus_store_sku");
        doThrow(new DataIntegrityViolationException("could not execute batch", hibernate)).when(repository).flush();

        assertThatThrownBy(() -> adapter.applyChanges(product(List.of()), new ProductChangeSet(true, false, List.of(), List.of(), List.of()), 2L))
                .isInstanceOf(SkuCodeInUseException.class);
    }

    @Test
    @DisplayName("currentVersion reads the stored version")
    void currentVersion_read() {
        when(repository.findVersion(productId, storeId)).thenReturn(Optional.of(7L));

        assertThat(adapter.currentVersion(StoreId.of(storeId), ProductId.of(productId))).isEqualTo(7L);
    }

    @Test
    @DisplayName("create inserts the SKUs in code order and maps a deadlock to 409")
    void create_sortsSkus_andMapsDeadlock() {
        StoreProduct product = product(List.of());
        StoreProductEntity entity = new StoreProductEntity();
        when(mapper.fromStoreProduct(product)).thenReturn(entity);
        when(mapper.fromProductSkus(any(), any())).thenReturn(List.of(skuEntity(null, "Z"), skuEntity(null, "A")));
        when(repository.saveAndFlush(entity)).thenThrow(new CannotAcquireLockException("deadlock"));

        assertThatThrownBy(() -> adapter.create(product)).isInstanceOf(SkuCodeInUseException.class);
        assertThat(entity.getSkus()).extracting(ProductSkuEntity::getSku).containsExactly("A", "Z");
    }

    @Test
    @DisplayName("create maps a SKU code collision to SkuCodeInUseException")
    void createCollision_mapped() {
        StoreProduct product = product(List.of());
        StoreProductEntity entity = entity();
        when(mapper.fromStoreProduct(product)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("dup",
                new SQLException("duplicate key value violates unique constraint \"uk_product_skus_store_sku\"")));

        assertThatThrownBy(() -> adapter.create(product)).isInstanceOf(SkuCodeInUseException.class);
    }

    @Test
    @DisplayName("bumpVersion is true only when the guarded UPDATE changed a row")
    void bumpVersion_mapsRowCount() {
        when(repository.bumpVersion(productId, storeId, 3L, NOW)).thenReturn(1);
        when(repository.bumpVersion(productId, storeId, 2L, NOW)).thenReturn(0);

        assertThat(adapter.bumpVersion(StoreId.of(storeId), ProductId.of(productId), 3L, NOW)).isTrue();
        assertThat(adapter.bumpVersion(StoreId.of(storeId), ProductId.of(productId), 2L, NOW)).isFalse();
    }

    @Test
    @DisplayName("SKU code checks map the codes and skip the query for an empty list")
    void codeChecks() {
        UUID otherStore = UUID.randomUUID();
        when(repository.findSkuCodesInStore(otherStore, List.of("A", "B"))).thenReturn(Set.of("B"));
        when(repository.findSkuCodesOfOtherProducts(storeId, productId, List.of("A"))).thenReturn(Set.of());

        assertThat(adapter.findSkuCodesInStore(StoreId.of(otherStore), List.of(SKU.of("A"), SKU.of("B"))))
                .extracting(SKU::getValue).containsExactly("B");
        assertThat(adapter.findSkuCodesOfOtherProducts(StoreId.of(storeId), ProductId.of(productId), List.of(SKU.of("A")))).isEmpty();
        assertThat(adapter.findSkuCodesInStore(StoreId.of(storeId), List.of())).isEmpty();
        assertThat(adapter.findSkuCodesOfOtherProducts(StoreId.of(storeId), ProductId.of(productId), List.of())).isEmpty();
        // only the two non-empty calls reached the database
        verify(repository, times(1)).findSkuCodesInStore(any(), anyList());
        verify(repository, times(1)).findSkuCodesOfOtherProducts(any(), any(), anyList());
    }
}
