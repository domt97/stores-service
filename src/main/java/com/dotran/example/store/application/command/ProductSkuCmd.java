package com.dotran.example.store.application.command;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSkuCmd {

    private String sku;

    private String name;

    private BigDecimal price;

    private String currency;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;
}