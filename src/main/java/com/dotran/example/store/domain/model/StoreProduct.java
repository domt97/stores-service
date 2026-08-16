package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.AggregateRoot;
import com.dotran.example.store.common.domain.valueobject.CategoryId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.StoreProductId;
import com.dotran.example.store.domain.enums.ProductStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StoreProduct extends AggregateRoot<StoreProductId> {

    private StoreId storeId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private CategoryId categoryId;
    private ProductStatus status;
    private List<ProductSku> skus;
    private List<ProductImage> images;
    private Instant createdAt;
    private Instant updatedAt;

    public void initState() {
        this.status = ProductStatus.ACTIVE;
        this.skus = this.skus != null ? this.skus : new ArrayList<>();
        this.images = this.images != null ? this.images : new ArrayList<>();
        this.createdAt = this.updatedAt = Instant.now();
    }

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

    public BigDecimal getMinPrice() {
        return Optional.ofNullable(skus)
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMaxPrice() {
        return Optional.ofNullable(skus)
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public String getCurrency() {
        return Optional.ofNullable(skus)
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getCurrency)
                .findFirst()
                .orElse(null);
    }

    public int getSkuCount() {
        return Optional.ofNullable(skus)
                .orElseGet(List::of)
                .size();
    }
}
