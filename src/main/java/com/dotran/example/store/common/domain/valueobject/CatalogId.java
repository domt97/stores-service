package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CatalogId extends BaseId<UUID> {

    public CatalogId(UUID value) {
        super(value);
    }

    public static CatalogId of(UUID value) {
        return new CatalogId(value);
    }

    public static CatalogId newCatalogId() {
        return new CatalogId(UUID.randomUUID());
    }
}
