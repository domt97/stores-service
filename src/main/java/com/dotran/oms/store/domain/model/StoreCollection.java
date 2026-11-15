package com.dotran.oms.store.domain.model;

import com.dotran.oms.store.common.constants.Constants;
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

    /**
     * Adding or removing products is idempotent (BR-CAT-15): products already in (or not in) the collection are skipped
     * by the write itself, which also makes concurrent requests safe. Only an empty request is refused.
     */
    public static void checkProductListChange(List<ProductId> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException(Constants.ERROR_MSG_COLLECTION_PRODUCT_LIST_EMPTY);
        }
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
