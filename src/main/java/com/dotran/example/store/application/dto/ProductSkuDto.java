package com.dotran.example.store.application.dto;

import com.dotran.example.store.domain.enums.ProductSkuStatus;
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
public class ProductSkuDto {

    private UUID id;
    private UUID productId;

    private String sku;
    private String name;

    private BigDecimal price;
    private String currency;

    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    private ProductSkuStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}
