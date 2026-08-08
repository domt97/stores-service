package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.BaseDomain;
import com.dotran.example.store.common.domain.valueobject.CatalogId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.StoreProductId;
import com.dotran.example.store.domain.enums.ProductStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class StoreProduct extends BaseDomain<StoreProductId> {

    private StoreId storeId;

    private String name;
    private String description;

    private CatalogId categoryId;

    private ProductStatus status;

    private List<ProductSku> skus = new ArrayList<>();
    private List<ProductImage> images = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public void activate() {
        this.status = ProductStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void addSku(ProductSku sku) {
        this.skus.add(sku);
        this.updatedAt = Instant.now();
    }

    public void addImage(ProductImage image) {
        this.images.add(image);
        this.updatedAt = Instant.now();
    }

}
