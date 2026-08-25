package com.dotran.example.store.infrastructure.persistence.jpa;

import com.dotran.example.store.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
