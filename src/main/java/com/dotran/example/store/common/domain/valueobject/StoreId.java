package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreId extends BaseId<UUID> {

    public StoreId(UUID value) {
        super(value);
    }
    public static StoreId of(UUID value) {
        return new StoreId(value);
    }


    public static StoreId newStoreId() {
        return new StoreId(UUID.randomUUID());
    }
}
