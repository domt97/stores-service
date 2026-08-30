package com.dotran.example.store.application.impex.product.column;

import lombok.Getter;

@Getter
public enum ProductImportColumn {

    REFERENCE(0, "Reference"),
    NAME(1, "Name"),
    DESCRIPTION(2, "Description"),
    THUMBNAIL_URL(3, "Thumbnail Url"),
    CATEGORY(4, "Category"),
    BRAND_ID(5, "Brand Id");

    private final int index;
    private final String columnName;

    ProductImportColumn(int index, String columnName) {
        this.index = index;
        this.columnName = columnName;
    }
}
