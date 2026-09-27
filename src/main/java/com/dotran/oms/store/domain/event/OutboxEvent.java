package com.dotran.oms.store.domain.event;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.store.common.id.EventId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OutboxEvent extends AggregateRoot<EventId> {

    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private Object payload;
    private OutboxStatus status;
    private Instant createdAt;
    private Instant publishedAt;
    private Integer retryCount;
    private String lastError;
}
