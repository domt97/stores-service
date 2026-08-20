package com.dotran.example.store.application.command;

import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RemoveProductCollectionCmd {

    private TenantId tenantId;
    private StoreId storeId;
    private StoreCollectionId storeCollectionId;
    private List<ProductId> productIds;
}
