package com.dotran.example.store.application.impex.product;

import com.dotran.example.store.common.domain.valueobject.SKU;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSKUImportRow {

    private String productRef;
    private SKU sku;
    private String name;
    private BigDecimal price;
    private String currency;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
}
