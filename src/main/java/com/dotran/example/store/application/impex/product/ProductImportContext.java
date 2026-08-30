package com.dotran.example.store.application.impex.product;

import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.model.StoreProduct;
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
