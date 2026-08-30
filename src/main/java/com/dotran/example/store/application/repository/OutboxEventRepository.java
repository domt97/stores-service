package com.dotran.example.store.application.repository;

import com.dotran.example.store.domain.event.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);

    void saveAll(List<OutboxEvent> outboxEvents);
}
