package com.dotran.oms.store.infrastructure.rest.dto.response;

import com.dotran.oms.store.domain.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class StoreProductPreviewResponse {

    private UUID id;
    private UUID storeId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private ProductStatus status;
    private Integer skuCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;
}
