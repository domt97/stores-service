package com.dotran.example.store.application.command.impex;

import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
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
