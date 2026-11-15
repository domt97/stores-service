package com.dotran.oms.store.application.impex.upload;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;

import java.time.Duration;

/**
 * Signs an upload of one import file into the store's import folder (BR-CAT-16/17). The adapter decides the bucket and
 * key (the same location the import reader reads) and binds the size, the .xlsx content type and the cleanup tag into
 * the signature, so storage rejects any other upload.
 */
public interface ImportUploadSigner {

    SignedUpload sign(MerchantId merchantId, StoreId storeId, ImportFileName fileName, long sizeBytes, Duration ttl);
}
