package com.dotran.oms.store.application.command.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.common.id.StoreCollectionId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCollectionDetailCmd {

    private TenantId tenantId;
    private StoreId storeId;
    private StoreCollectionId storeCollectionId;
}
