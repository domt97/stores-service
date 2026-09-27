package com.dotran.oms.store.application.repository;

import com.dotran.oms.store.domain.event.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);

    void saveAll(List<OutboxEvent> outboxEvents);
}
