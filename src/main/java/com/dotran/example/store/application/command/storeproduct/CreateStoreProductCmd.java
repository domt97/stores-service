package com.dotran.example.store.application.command.storeproduct;

import com.dotran.example.store.common.domain.valueobject.CategoryId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
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