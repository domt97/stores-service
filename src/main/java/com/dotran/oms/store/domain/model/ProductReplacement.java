package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.store.domain.enums.ProductStatus;

import java.util.List;

/**
 * The full requested state of a product (update-product is a full replace): omitted optional fields are cleared,
 * SKUs left out are removed, and the image list replaces the current one.
 */
public record ProductReplacement(String name, String description, String thumbnailUrl, CategoryId categoryId,
                                 ProductStatus status, List<ProductSkuSpec> skus, List<ImageSpec> images) {

    public record ImageSpec(String imageUrl, Integer displayOrder) {
    }
}
