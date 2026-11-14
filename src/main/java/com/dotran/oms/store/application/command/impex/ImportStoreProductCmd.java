package com.dotran.oms.store.application.command.impex;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportStoreProductCmd {

    /** Name of the file in the store's import folder (BR-CAT-16); checked by ImportFileName. */
    private String fileName;
    private MerchantId merchantId;
    private StoreId storeId;
}
