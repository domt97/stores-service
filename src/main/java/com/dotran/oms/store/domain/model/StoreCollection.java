package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
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

    public void addProduct(ProductId productId) {
        if (null == productId) {
            return;
        }

        if (this.productIds == null) {
            this.productIds = new ArrayList<>();
        }

        this.productIds.add(productId);
        this.updatedAt = Instant.now();
    }

    public boolean canAddProductsToCollection(List<ProductId> newProductIds) {
        if(null == newProductIds || newProductIds.isEmpty()) {
            throw new BusinessException("Product list cannot be null or empty");
        }

        for(ProductId newProductId : newProductIds) {
            if (this.productIds.contains(newProductId)) {
                throw new BusinessException("Product already exists in the collection");
            }
        }

        return true;
    }

    public boolean canRemoveProductsFromCollection(List<ProductId> productIdsToRemove) {
        if(null == productIdsToRemove || productIdsToRemove.isEmpty()) {
            throw new BusinessException("Product list cannot be null or empty");
        }

        for(ProductId productIdToRemove : productIdsToRemove) {
            if (!this.productIds.contains(productIdToRemove)) {
                throw new BusinessException("Product does not exist in the collection");
            }
        }

        return true;
    }

    public void removeProduct(List<ProductId> productIdsToRemove) {
        for(ProductId productId : productIdsToRemove) {
            this.removeProduct(productId);
        }
        this.updatedAt = Instant.now();
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
