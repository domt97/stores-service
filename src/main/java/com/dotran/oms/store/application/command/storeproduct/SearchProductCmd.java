package com.dotran.oms.store.application.command.storeproduct;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.common.id.StoreCollectionId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchProductCmd {

    private MerchantId merchantId;
    private StoreId storeId;
    private PriceRange priceRange;
    /** Optional: leave out the products already in this collection (it must belong to the store). */
    private StoreCollectionId notInCollectionId;
}
