package com.dotran.example.store.application.dto;

import com.dotran.example.store.domain.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductReviewDto {

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
