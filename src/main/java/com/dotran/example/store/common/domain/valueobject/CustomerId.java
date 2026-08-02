package com.dotran.example.store.common.domain.valueobject;

import java.util.UUID;

public class CustomerId extends BaseId<UUID> {

    public CustomerId(UUID value) {
        super(value);
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId newCustomerId() {
        return new CustomerId(UUID.randomUUID());
    }
}
