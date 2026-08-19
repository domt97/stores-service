package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreCollectionId extends BaseId<UUID> {

    public StoreCollectionId(UUID value) {
        super(value);
    }

    public static StoreCollectionId of(UUID value) {
        return new StoreCollectionId(value);
    }

    public static StoreCollectionId generateId() {
        return new StoreCollectionId(UUID.randomUUID());
    }
}
