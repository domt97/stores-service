package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.AggregateRoot;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.StoreProductId;
import com.dotran.example.store.domain.enums.CollectionStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StoreCollection extends AggregateRoot<StoreCollectionId> {

    private StoreId storeId;
    private String name;
    private String description;
    private CollectionStatus status;
    private List<StoreProductId> productIds;
    private Instant createdAt;
    private Instant updatedAt;


    public void addProduct(StoreProductId productId) {
        this.productIds.add(productId);
        this.updatedAt = Instant.now();
    }

    public void removeProduct(StoreProductId productId) {
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
}
