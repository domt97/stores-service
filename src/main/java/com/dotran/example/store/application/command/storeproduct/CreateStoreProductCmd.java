package com.dotran.example.store.application.command.storeproduct;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreateStoreProductCmd {

    private UUID tenantId;

    private UUID storeId;

    private String name;

    private String description;

    private UUID categoryId;

    private UUID brandId;

    private List<ProductSkuCmd> skus;

    private List<ProductImageCmd> images;
}