package com.dotran.oms.store.infrastructure.mapper;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductSkuEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreProductPersistenceMapper — update-product fields")
class StoreProductPersistenceMapperUpdateTest {

    private final StoreProductPersistenceMapper mapper = new StoreProductPersistenceMapperImpl();

    @Test
    @DisplayName("a SKU entity takes its store from the product (uk_product_skus_store_sku) and keeps removedAt")
    void skuEntity_storeFromProduct() {
        StoreProductEntity product = new StoreProductEntity();
        product.setId(UUID.randomUUID());
        product.setStoreId(UUID.randomUUID());
        Instant removedAt = Instant.parse("2026-10-29T10:00:00Z");
        ProductSku sku = ProductSku.builder().sku(SKU.of("A")).price(BigDecimal.ONE).currency("VND")
                .status(ProductSkuStatus.ACTIVE).removedAt(removedAt).build();

        ProductSkuEntity entity = mapper.fromProductSku(sku, product);

        assertThat(entity.getStoreId()).isEqualTo(product.getStoreId());
        assertThat(entity.getProduct()).isSameAs(product);
        assertThat(entity.getRemovedAt()).isEqualTo(removedAt);
        assertThat(entity.getId()).isNull();
    }

    @Test
    @DisplayName("the product version is never copied onto the entity (only the guarded bump writes it)")
    void productVersion_notMapped() {
        StoreProduct product = StoreProduct.builder().id(ProductId.newProductId()).storeId(StoreId.newStoreId())
                .name("P").version(9).build();

        assertThat(mapper.fromStoreProduct(product).getVersion()).isZero();
    }
}
