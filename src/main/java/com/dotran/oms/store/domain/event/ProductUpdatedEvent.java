package com.dotran.oms.store.domain.event;

import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Catalog snapshot of a product after an update (oms-workspace contracts/events/ProductUpdated.schema.json).
 * Consumed by inventory-service; keep its DTO in sync. SKU codes are plain strings, removed SKUs are left out.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductUpdatedEvent {

    private UUID eventId;
    @Builder.Default
    private String eventType = "ProductUpdated";
    private Instant occurredAt;

    private UUID merchantId;
    private UUID storeId;
    private UUID productId;

    /** Strictly increasing per product, ≥ 1 (ProductCreated is version 0). */
    private long version;
    private ProductStatus status;
    private String name;
    private UUID categoryId;

    private List<Sku> skus;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Sku {

        private UUID id;
        private String sku;
        private String name;
        private ProductSkuStatus status;
        private BigDecimal price;
        private String currency;
    }
}
