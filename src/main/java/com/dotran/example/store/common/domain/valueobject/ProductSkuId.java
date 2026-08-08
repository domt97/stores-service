package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductSkuId extends BaseId<UUID> {

    public ProductSkuId(UUID value) {
        super(value);
    }

    public static ProductSkuId of(UUID value) {
        return new ProductSkuId(value);
    }

    public static ProductSkuId newProductSkuId() {
        return new ProductSkuId(UUID.randomUUID());
    }
}
