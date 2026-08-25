package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.OutboxEventRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.domain.event.OutboxEvent;
import com.dotran.example.store.infrastructure.persistence.entity.OutboxEventEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

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
        entity.setPayload(objectMapper.valueToTree(outboxEvent.getPayload()));
        entity.setStatus(outboxEvent.getStatus());
        entity.setRetryCount(outboxEvent.getRetryCount());
        entity.setCreatedAt(outboxEvent.getCreatedAt());

        repository.save(entity);
    }
}
