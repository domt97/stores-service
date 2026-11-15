package com.dotran.oms.store.application.impex.parser;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.product.ProductImportData;

/**
 * Reads a product import file from the store's own import folder (BR-CAT-16). The adapter decides where that folder is
 * (bucket and key layout); the caller can only name a file inside it.
 */
public interface StoreProductImportReader {

    ProductImportData read(MerchantId merchantId, StoreId storeId, ImportFileName fileName);
}
