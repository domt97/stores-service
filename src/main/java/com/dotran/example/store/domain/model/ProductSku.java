package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.BaseDomain;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.ProductSkuId;
import com.dotran.example.store.common.domain.valueobject.SKU;
import com.dotran.example.store.domain.enums.ProductSkuStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ProductSku extends BaseDomain<ProductSkuId> {

    private ProductId productId;

    private SKU sku;
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

    public void init() {
        this.status = ProductSkuStatus.ACTIVE;
        this.createdAt = this.updatedAt = Instant.now();
    }
}
