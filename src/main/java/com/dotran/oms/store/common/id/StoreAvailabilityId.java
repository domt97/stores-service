package com.dotran.oms.store.common.id;

import com.dotran.oms.core.domain.id.BaseId;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreAvailabilityId extends BaseId<UUID> {

    public StoreAvailabilityId(UUID value) {
        super(value);
    }

    public static StoreAvailabilityId of(UUID value) {
        return new StoreAvailabilityId(value);
    }

    public static StoreAvailabilityId generateId() {
        return new StoreAvailabilityId(UUID.randomUUID());
    }
}
