package com.dotran.example.store.application.impex.product.column;

import lombok.Getter;

@Getter
public enum ProductSKUColumn {

    PRODUCT_REF(0, "Product Ref"),
    SKU(1, "SKU"),
    NAME(2, "Name"),
    PRICE(3, "Price"),
    CURRENCY(4, "Currency"),
    WEIGHT(5, "Weight"),
    LENGTH(6, "Length"),
    WIDTH(7, "Width"),
    HEIGHT(8, "Height");

    private final int index;
    private final String columnName;

    ProductSKUColumn(int index, String columnName) {
        this.index = index;
        this.columnName = columnName;
    }
}
