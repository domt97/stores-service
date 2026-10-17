package com.dotran.oms.store.domain.event;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode(of = "id")
public class OutboxEvent {

    private final EventId id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final Object payload;
    private final OutboxStatus status;
    private final Instant createdAt;
    private final Instant publishedAt;
    private final Integer retryCount;
    private final String lastError;
}
