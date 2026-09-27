package com.dotran.oms.store.common.id;

import com.dotran.oms.core.domain.id.BaseId;

import java.util.UUID;

public class EventId extends BaseId<UUID> {

    public EventId(UUID value) {
        super(value);
    }

    public static EventId of(UUID value) {
        return new EventId(value);
    }

    public static EventId newEventId() {
        return new EventId(UUID.randomUUID());
    }
}
