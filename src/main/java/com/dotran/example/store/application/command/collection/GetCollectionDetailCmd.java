package com.dotran.example.store.application.command.collection;

import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCollectionDetailCmd {

    private TenantId tenantId;
    private StoreId storeId;
    private StoreCollectionId storeCollectionId;
}
