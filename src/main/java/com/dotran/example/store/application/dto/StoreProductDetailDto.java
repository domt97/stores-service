package com.dotran.example.store.application.dto;

import com.dotran.example.store.domain.enums.ProductStatus;
import com.dotran.example.store.domain.model.ProductSku;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductDetailDto {

    private UUID id;
    private UUID storeId;
    private String name;
    private String description;
    private UUID categoryId;
    private ProductStatus status;
    private List<ProductSkuDto> skus = new ArrayList<>();
    private List<ProductImageDto> images = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
