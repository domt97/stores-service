package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.infrastructure.persistence.entity.OutboxEventEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPersistenceAdapter")
class OutboxEventPersistenceAdapterTest {

    @Mock private SpringDataOutboxEventRepository repository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventPersistenceAdapter adapter;

    private OutboxEvent buildEvent() {
        return OutboxEvent.builder()
                .id(EventId.newEventId())
                .aggregateType("Product")
                .aggregateId(UUID.randomUUID())
                .eventType("ProductCreated")
                .payload("payload-data")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("save: creates entity with correct field values and persists it")
    void save_createsEntityWithCorrectFields() {
        OutboxEvent event = buildEvent();
        when(objectMapper.valueToTree(any())).thenReturn(new TextNode("serialized"));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);

        adapter.save(event);

        verify(repository).save(captor.capture());
        OutboxEventEntity entity = captor.getValue();

        assertThat(entity.getId()).isEqualTo(event.getId().getValue());
        assertThat(entity.getAggregateType()).isEqualTo("Product");
        assertThat(entity.getAggregateId()).isEqualTo(event.getAggregateId());
        assertThat(entity.getEventType()).isEqualTo("ProductCreated");
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("saveAll: creates one entity per event and passes them to the repository")
    void saveAll_createsOneEntityPerEvent() {
        OutboxEvent event1 = buildEvent();
        OutboxEvent event2 = buildEvent();
        when(objectMapper.valueToTree(any())).thenReturn(new TextNode("serialized"));

        adapter.saveAll(List.of(event1, event2));

        ArgumentCaptor<List<OutboxEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAllAndFlush(captor.capture());
        verify(repository, never()).saveAll(any());
        assertThat(captor.getValue()).hasSize(2);
    }
}
