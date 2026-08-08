package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductId extends BaseId<UUID> {

    public ProductId(UUID value) {
        super(value);
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    public static ProductId newProductId() {
        return new ProductId(UUID.randomUUID());
    }
}
