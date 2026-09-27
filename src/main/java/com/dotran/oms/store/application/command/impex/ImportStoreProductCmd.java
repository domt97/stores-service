package com.dotran.oms.store.application.command.impex;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportStoreProductCmd {

    private String path;
    private String fileName;
    private TenantId tenantId;
    private StoreId storeId;
}
