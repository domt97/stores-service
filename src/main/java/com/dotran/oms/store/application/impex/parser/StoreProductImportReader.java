package com.dotran.oms.store.application.impex.parser;

import com.dotran.oms.store.application.impex.product.ProductImportData;

public interface StoreProductImportReader {

    ProductImportData read(String path, String fileName);
}
