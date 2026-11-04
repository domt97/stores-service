package com.dotran.oms.store.domain.event;

import com.dotran.oms.core.domain.id.SKU;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCreatedEvent {

    private UUID eventId;
    @Builder.Default
    private String eventType = "ProductCreated";
    private Instant occurredAt;

    private UUID merchantId;
    private UUID storeId;
    private UUID productId;

    /** Always 0: lets consumers order ProductCreated against ProductUpdated (version ≥ 1). */
    @Builder.Default
    private Long version = 0L;

    private List<SKU> skus;
}
