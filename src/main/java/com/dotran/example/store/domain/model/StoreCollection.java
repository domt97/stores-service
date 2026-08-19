package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.AggregateRoot;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.domain.enums.CollectionStatus;
import com.dotran.example.store.domain.exception.BusinessException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StoreCollection extends AggregateRoot<StoreCollectionId> {

    private StoreId storeId;
    private String name;
    private String description;
    private CollectionStatus status;
    private List<ProductId> productIds;
    private Instant createdAt;
    private Instant updatedAt;


    public void init() {
        this.status = CollectionStatus.ACTIVE;
        this.createdAt = this.updatedAt = Instant.now();
    }

    public void addProduct(StoreProduct product) {
        if (null == product) {
            return;
        }

        if (!this.storeId.equals(product.getStoreId())) {
            throw new BusinessException("Cannot add product from a different store to the collection");
        }

        if (this.productIds == null) {
            this.productIds = new ArrayList<>();
        }

        this.productIds.add(product.getId());
        this.updatedAt = Instant.now();
    }

    public void addProducts(List<StoreProduct> products) {
        for (StoreProduct product : products) {
            this.addProduct(product);
        }

        this.updatedAt = Instant.now();
    }

    public boolean canAddProductsToCollection(List<ProductId> newProductIds) {
        for(ProductId newProductId : newProductIds) {
            if (this.productIds.contains(newProductId)) {
                throw new BusinessException("Product already exists in the collection");
            }
        }

        return true;
    }

    public void removeProduct(ProductId productId) {
        this.productIds.remove(productId);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = CollectionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CollectionStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void update() {
        this.updatedAt = Instant.now();
    }

    public Integer getProductCount() {
        return this.productIds != null ? this.productIds.size() : 0;
    }
}
