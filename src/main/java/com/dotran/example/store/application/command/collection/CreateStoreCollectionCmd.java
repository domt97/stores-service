package com.dotran.example.store.application.command.collection;

import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
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
