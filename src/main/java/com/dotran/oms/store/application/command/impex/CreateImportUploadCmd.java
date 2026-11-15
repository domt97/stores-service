package com.dotran.oms.store.application.command.impex;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import lombok.Builder;

/** Ask for a presigned upload of one import file into the store's import folder (BR-CAT-17). */
@Builder
public record CreateImportUploadCmd(MerchantId merchantId, StoreId storeId, long sizeBytes) {
}
