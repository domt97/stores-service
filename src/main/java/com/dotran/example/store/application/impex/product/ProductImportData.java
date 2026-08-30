package com.dotran.example.store.application.impex.product;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImportData {

    private List<ProductImportRow> products;
    private List<ProductSKUImportRow> skus;
    private List<ProductImageImportRow> images;
}
