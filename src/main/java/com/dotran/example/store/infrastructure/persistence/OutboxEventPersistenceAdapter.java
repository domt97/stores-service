package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.OutboxEventRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.domain.event.OutboxEvent;
import com.dotran.example.store.infrastructure.persistence.entity.OutboxEventEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@PersistenceAdapter
@RequiredArgsConstructor
public class OutboxEventPersistenceAdapter implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = new OutboxEventEntity();

        entity.setId(outboxEvent.getId().getValue());
        entity.setAggregateType(outboxEvent.getAggregateType());
        entity.setAggregateId(outboxEvent.getAggregateId());
        entity.setEventType(outboxEvent.getEventType());
        try {
            entity.setPayload(objectMapper.writeValueAsString(outboxEvent.getPayload()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
        entity.setStatus(outboxEvent.getStatus());
        entity.setCreatedAt(Instant.now());
        entity.setRetryCount(outboxEvent.getRetryCount());
        entity.setCreatedAt(outboxEvent.getCreatedAt());

        repository.save(entity);
    }
}
