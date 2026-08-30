package com.dotran.example.store.application.impex.parser;

import com.dotran.example.store.application.impex.product.ProductImportData;

public interface StoreProductImportReader {

    ProductImportData read(String path, String fileName);
}
