package com.dotran.example.store.application.command.storeproduct;

import com.dotran.example.store.common.domain.valueobject.SKU;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSkuCmd {

    private SKU sku;

    private String name;

    private BigDecimal price;

    private String currency;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;
}