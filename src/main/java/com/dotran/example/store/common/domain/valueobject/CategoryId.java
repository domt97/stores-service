package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryId extends BaseId<UUID> {

    public CategoryId(UUID value) {
        super(value);
    }

    public static CategoryId of(UUID value) {
        return new CategoryId(value);
    }

    public static CategoryId newCatalogId() {
        return new CategoryId(UUID.randomUUID());
    }
}
