package com.dotran.oms.store.application.command.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateStoreCollectionCmd {

    private TenantId tenantId;
    private StoreId storeId;
    private String name;
    private String description;
    private List<ProductId> productIds;
}
