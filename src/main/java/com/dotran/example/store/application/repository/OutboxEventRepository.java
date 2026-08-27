package com.dotran.example.store.application.repository;

import com.dotran.example.store.domain.event.OutboxEvent;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);
}
