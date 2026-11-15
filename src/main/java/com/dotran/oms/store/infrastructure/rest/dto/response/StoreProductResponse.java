package com.dotran.oms.store.infrastructure.rest.dto.response;

import com.dotran.oms.store.application.dto.ProductImageDto;
import com.dotran.oms.store.application.dto.ProductSkuDto;
import com.dotran.oms.store.domain.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class StoreProductResponse {

    private UUID id;
    private UUID storeId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private UUID categoryId;
    private ProductStatus status;
    private List<ProductSkuDto> skus;
    private List<ProductImageDto> images;
    /** Send back in the update-product PUT. */
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
}
