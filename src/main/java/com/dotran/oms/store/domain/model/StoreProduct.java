package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;

import com.dotran.oms.store.domain.enums.ProductStatus;
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
public class StoreProduct extends AggregateRoot<ProductId> {

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
        // Own mutable copies: callers may pass immutable lists (e.g. Stream.toList() in the mapper).
        this.skus = this.skus != null ? new ArrayList<>(this.skus) : new ArrayList<>();
        this.images = this.images != null ? new ArrayList<>(this.images) : new ArrayList<>();
        this.skus.forEach(ProductSku::normaliseForSale);
        this.status = ProductStatus.ACTIVE;
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
        sku.normaliseForSale();
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

    public List<SKU> getListOfSKUs() {
        return Optional.ofNullable(skus)
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getSku)
                .toList();
    }

}
