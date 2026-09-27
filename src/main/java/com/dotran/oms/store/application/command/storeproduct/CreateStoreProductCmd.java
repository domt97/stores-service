package com.dotran.oms.store.application.command.storeproduct;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreateStoreProductCmd {

    private TenantId tenantId;

    private StoreId storeId;

    private String name;

    private String description;

    private CategoryId categoryId;

    private UUID brandId;

    private List<ProductSkuCmd> skus;

    private List<ProductImageCmd> images;
}