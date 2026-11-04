package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.infrastructure.persistence.entity.OutboxEventEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.dotran.oms.core.logging.MdcProperties;
import com.dotran.oms.core.logging.MdcPropagation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

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

    private OutboxEventPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MdcProperties mdcProperties = new MdcProperties();
        mdcProperties.validate();
        adapter = new OutboxEventPersistenceAdapter(repository, objectMapper, new MdcPropagation(mdcProperties));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

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

    @Test
    @DisplayName("save: the request's propagated MDC keys go into correlation_context")
    void save_capturesCorrelationContext() {
        when(objectMapper.valueToTree(any())).thenReturn(new TextNode("serialized"));
        MDC.put("requestId", "abc-123");
        MDC.put("storeId", "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");
        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);

        adapter.save(buildEvent());

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCorrelationContext()).containsExactly(java.util.Map.entry("requestId", "abc-123"));
    }

    @Test
    @DisplayName("save: no MDC gives an empty correlation_context, never null")
    void save_emptyCorrelationContextWithoutMdc() {
        when(objectMapper.valueToTree(any())).thenReturn(new TextNode("serialized"));
        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);

        adapter.save(buildEvent());

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCorrelationContext()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("saveAll: every entity gets the same correlation_context")
    @SuppressWarnings("unchecked")
    void saveAll_setsCorrelationContextOnEveryEntity() {
        when(objectMapper.valueToTree(any())).thenReturn(new TextNode("serialized"));
        MDC.put("requestId", "import-1");
        ArgumentCaptor<List<OutboxEventEntity>> captor = ArgumentCaptor.forClass(List.class);

        adapter.saveAll(List.of(buildEvent(), buildEvent()));

        verify(repository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .allSatisfy(e -> assertThat(e.getCorrelationContext()).containsExactly(java.util.Map.entry("requestId", "import-1")));
    }
}
