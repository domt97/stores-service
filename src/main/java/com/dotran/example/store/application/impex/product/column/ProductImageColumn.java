package com.dotran.example.store.application.impex.product.column;

import lombok.Getter;

@Getter
public enum ProductImageColumn {

    PRODUCT_REF(0, "Product Ref"),
    URL(1, "URL");

    private final int index;
    private final String columnName;


    ProductImageColumn(int index, String columnName) {
        this.index = index;
        this.columnName = columnName;
    }
}
