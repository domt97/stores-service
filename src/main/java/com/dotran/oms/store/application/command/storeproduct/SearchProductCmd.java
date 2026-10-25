package com.dotran.oms.store.application.command.storeproduct;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.valueobject.PriceRange;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchProductCmd {

    private MerchantId merchantId;
    private StoreId storeId;
    private PriceRange priceRange;
}
