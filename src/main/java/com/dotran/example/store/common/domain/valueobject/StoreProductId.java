package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreProductId extends BaseId<UUID> {

    public StoreProductId(UUID value) {
        super(value);
    }

    public static StoreProductId of(UUID value) {
        return new StoreProductId(value);
    }

    public static StoreProductId newStoreProductId() {
        return new StoreProductId(UUID.randomUUID());
    }
}
