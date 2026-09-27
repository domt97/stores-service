package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductImportContext {

    private TenantId tenantId;
    private StoreId storeId;
    private List<StoreProduct> products;
}
