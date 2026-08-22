package com.dotran.example.store.application.command.storeproduct;

import com.dotran.example.store.common.domain.valueobject.PriceRange;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchProductCmd {

    private TenantId tenantId;
    private StoreId storeId;
    private PriceRange priceRange;
}
