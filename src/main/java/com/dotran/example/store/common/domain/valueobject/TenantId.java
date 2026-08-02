package com.dotran.example.store.common.domain.valueobject;

import java.util.UUID;

public class TenantId extends BaseId<UUID> {

    public TenantId(UUID value) {
        super(value);
    }

    public static TenantId of(UUID value) {
        return new TenantId(value);
    }

    public static TenantId newTenantId() {
        return new TenantId(UUID.randomUUID());
    }
}
