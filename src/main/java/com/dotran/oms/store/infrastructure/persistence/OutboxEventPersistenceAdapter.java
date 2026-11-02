package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.infrastructure.persistence.entity.OutboxEventEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPersistenceAdapter implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = createOutboxEventEntity(outboxEvent);

        repository.save(entity);
        logQueued(outboxEvent);
    }

    @Override
    public void saveAll(List<OutboxEvent> outboxEvents) {
        List<OutboxEventEntity> entities = outboxEvents.stream().map(this::createOutboxEventEntity).toList();

        // Flush now: inside the import transaction a failing insert surfaces here, not later at commit.
        repository.saveAllAndFlush(entities);
        outboxEvents.forEach(this::logQueued);
    }

    /**
     * Joins this request's logs (requestId in the MDC) with the consumer's: the consumer logs the same eventId.
     * "Queued", not "published": the row is only visible after the transaction commits.
     */
    private void logQueued(OutboxEvent outboxEvent) {
        log.debug("Outbox event queued: eventType={} eventId={} aggregateId={}",
                outboxEvent.getEventType(), outboxEvent.getId().getValue(), outboxEvent.getAggregateId());
    }

    private OutboxEventEntity createOutboxEventEntity(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = new OutboxEventEntity();

        entity.setId(outboxEvent.getId().getValue());
        entity.setAggregateType(outboxEvent.getAggregateType());
        entity.setAggregateId(outboxEvent.getAggregateId());
        entity.setEventType(outboxEvent.getEventType());
        entity.setPayload(objectMapper.valueToTree(outboxEvent.getPayload()));
        entity.setStatus(outboxEvent.getStatus());
        entity.setRetryCount(outboxEvent.getRetryCount());
        entity.setCreatedAt(outboxEvent.getCreatedAt());

        return entity;
    }
}
