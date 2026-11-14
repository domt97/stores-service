package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.core.logging.MdcProperties;
import com.dotran.oms.core.logging.MdcPropagation;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.event.ProductUpdatedEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPersistenceAdapter")
class OutboxEventPersistenceAdapterTest {

    /** Close to Spring Boot's ObjectMapper (ISO dates), as in ProductCreatedEventJsonTest. */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Mock private NamedParameterJdbcTemplate jdbc;

    private MdcPropagation mdcPropagation;
    private OutboxEventPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MdcProperties mdcProperties = new MdcProperties();
        mdcProperties.validate();
        mdcPropagation = new MdcPropagation(mdcProperties);
        adapter = new OutboxEventPersistenceAdapter(jdbc, objectMapper, mdcPropagation);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private OutboxEvent buildEvent(Object payload) {
        return OutboxEvent.builder()
                .id(EventId.newEventId())
                .aggregateType("Product")
                .aggregateId(UUID.randomUUID())
                .eventType("ProductCreated")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
    }

    private OutboxEvent buildEvent() {
        return buildEvent(Map.of("name", "Product"));
    }

    private void insertsSucceed(int rows) {
        int[] counts = new int[rows];
        java.util.Arrays.fill(counts, 1);
        when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class))).thenReturn(counts);
    }

    private SqlParameterSource[] capturedInsertRows() {
        ArgumentCaptor<SqlParameterSource[]> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbc).batchUpdate(anyString(), captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<List<UUID>> capturedDeleteChunks(int calls) {
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jdbc, times(calls)).update(eq("DELETE FROM outbox_events WHERE id IN (:ids)"), captor.capture());
        return captor.getAllValues().stream().map(params -> new ArrayList<>((Collection<UUID>) params.get("ids")))
                .map(list -> (List<UUID>) list).toList();
    }

    @Test
    @DisplayName("save: inserts the row with id = eventId, the envelope values and the payload, then deletes it")
    void save_insertsThenDeletesWithEventId() throws Exception {
        OutboxEvent event = buildEvent();
        insertsSucceed(1);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        adapter.save(event);

        SqlParameterSource row = capturedInsertRows()[0];
        assertThat(row.getValue("id")).isEqualTo(event.getId().getValue());
        assertThat(row.getValue("aggregateType")).isEqualTo("Product");
        assertThat(row.getValue("aggregateId")).isEqualTo(event.getAggregateId());
        assertThat(row.getValue("eventType")).isEqualTo("ProductCreated");
        assertThat(row.getValue("status")).isInstanceOf(String.class).isEqualTo("PENDING");
        assertThat(row.getValue("retryCount")).isEqualTo(0);
        assertThat(objectMapper.readTree((String) row.getValue("payload")).get("name").asText()).isEqualTo("Product");
        assertThat(capturedDeleteChunks(1)).containsExactly(List.of(event.getId().getValue()));
    }

    @Test
    @DisplayName("saveAll: one batch insert for all events, one delete with the same ids")
    void saveAll_batchInsertAndDeleteSameIds() {
        OutboxEvent e1 = buildEvent();
        OutboxEvent e2 = buildEvent();
        insertsSucceed(2);
        when(jdbc.update(anyString(), anyMap())).thenReturn(2);

        adapter.saveAll(List.of(e1, e2));

        assertThat(capturedInsertRows()).extracting(r -> r.getValue("id"))
                .containsExactly(e1.getId().getValue(), e2.getId().getValue());
        assertThat(capturedDeleteChunks(1)).containsExactly(List.of(e1.getId().getValue(), e2.getId().getValue()));
    }

    @Test
    @DisplayName("saveAll: more ids than one chunk are deleted in several statements, all counted")
    void saveAll_deletesInChunks() {
        int total = OutboxEventPersistenceAdapter.DELETE_CHUNK + 1;
        List<OutboxEvent> events = IntStream.range(0, total).mapToObj(i -> buildEvent()).toList();
        insertsSucceed(total);
        when(jdbc.update(anyString(), anyMap())).thenReturn(OutboxEventPersistenceAdapter.DELETE_CHUNK, 1);

        adapter.saveAll(events);

        List<List<UUID>> chunks = capturedDeleteChunks(2);
        assertThat(chunks.get(0)).hasSize(OutboxEventPersistenceAdapter.DELETE_CHUNK);
        assertThat(chunks.get(1)).containsExactly(events.get(total - 1).getId().getValue());
    }

    @Test
    @DisplayName("saveAll: an empty list runs no SQL (a native IN () would fail)")
    void saveAll_emptyListNoSql() {
        adapter.saveAll(List.of());

        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("saveAll: an insert that wrote no row fails and nothing is deleted")
    void saveAll_shortInsertFails() {
        when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class))).thenReturn(new int[]{1, 0});

        assertThatThrownBy(() -> adapter.saveAll(List.of(buildEvent(), buildEvent())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("insert");
        verify(jdbc, never()).update(anyString(), anyMap());
    }

    @Test
    @DisplayName("saveAll: SUCCESS_NO_INFO insert counts are accepted (rewritten batches)")
    void saveAll_successNoInfoAccepted() {
        when(jdbc.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[]{Statement.SUCCESS_NO_INFO, Statement.SUCCESS_NO_INFO});
        when(jdbc.update(anyString(), anyMap())).thenReturn(2);

        adapter.saveAll(List.of(buildEvent(), buildEvent()));

        capturedDeleteChunks(1);
    }

    @Test
    @DisplayName("saveAll: a delete that removed fewer rows than inserted fails")
    void saveAll_shortDeleteFails() {
        insertsSucceed(2);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        assertThatThrownBy(() -> adapter.saveAll(List.of(buildEvent(), buildEvent())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("delete removed 1 of 2");
    }

    @Test
    @DisplayName("saveAll: a payload that cannot be serialised fails before any SQL")
    void saveAll_payloadSerializationFailureNoSql() {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.valueToTree(any())).thenThrow(new IllegalArgumentException("boom"));
        adapter = new OutboxEventPersistenceAdapter(jdbc, failing, mdcPropagation);

        assertThatThrownBy(() -> adapter.saveAll(List.of(buildEvent())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("serialization");
        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("saveAll: a correlation context that cannot be serialised fails before any SQL")
    void saveAll_correlationSerializationFailureNoSql() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        adapter = new OutboxEventPersistenceAdapter(jdbc, failing, mdcPropagation);

        assertThatThrownBy(() -> adapter.saveAll(List.of(buildEvent())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("serialization");
        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("save: the request's propagated MDC keys go into correlation_context")
    void save_capturesCorrelationContext() throws Exception {
        insertsSucceed(1);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);
        MDC.put("requestId", "abc-123");
        MDC.put("storeId", "3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab");

        adapter.save(buildEvent());

        JsonNode correlation = objectMapper.readTree((String) capturedInsertRows()[0].getValue("correlation"));
        assertThat(correlation.size()).isEqualTo(1);
        assertThat(correlation.get("requestId").asText()).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("save: no MDC gives an empty correlation_context, never null")
    void save_emptyCorrelationContextWithoutMdc() {
        insertsSucceed(1);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        adapter.save(buildEvent());

        assertThat(capturedInsertRows()[0].getValue("correlation")).isEqualTo("{}");
    }

    @Test
    @DisplayName("saveAll: every row gets the same correlation_context")
    void saveAll_sameCorrelationContextOnEveryRow() {
        insertsSucceed(2);
        when(jdbc.update(anyString(), anyMap())).thenReturn(2);
        MDC.put("requestId", "import-1");

        adapter.saveAll(List.of(buildEvent(), buildEvent()));

        assertThat(capturedInsertRows()).extracting(r -> r.getValue("correlation"))
                .containsOnly("{\"requestId\":\"import-1\"}");
    }

    @Test
    @DisplayName("payload: a price 120000.00 is written without trailing zeros, as the former JsonNode mapping did")
    void payload_bigDecimalTrailingZerosStripped() throws Exception {
        ProductUpdatedEvent payload = ProductUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.parse("2026-11-09T10:00:00Z"))
                .skus(List.of(ProductUpdatedEvent.Sku.builder()
                        .id(UUID.randomUUID()).sku("SKU-1").price(new BigDecimal("120000.00")).currency("VND").build()))
                .build();
        insertsSucceed(1);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        adapter.save(buildEvent(payload));

        BigDecimal price = rawPrice((String) capturedInsertRows()[0].getValue("payload"));
        assertThat(price).isEqualByComparingTo("120000");
        assertThat(price.scale()).isLessThanOrEqualTo(0);
    }

    /** The raw number token of "price" (a tree parse would normalise it again and hide a scale of 2). */
    private BigDecimal rawPrice(String json) throws Exception {
        try (JsonParser parser = objectMapper.getFactory().createParser(json)) {
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token == JsonToken.FIELD_NAME && "price".equals(parser.currentName())) {
                    parser.nextToken();
                    return new BigDecimal(parser.getText());
                }
            }
        }
        throw new AssertionError("no price in " + json);
    }

    @Test
    @DisplayName("save and saveAll require a running transaction (MANDATORY): the rows commit with the product change")
    void writesRequireTransaction() throws Exception {
        for (String name : List.of("save", "saveAll")) {
            Class<?> param = name.equals("save") ? OutboxEvent.class : List.class;
            Transactional tx = OutboxEventPersistenceAdapter.class.getMethod(name, param).getAnnotation(Transactional.class);
            assertThat(tx).as(name).isNotNull();
            assertThat(tx.propagation()).as(name).isEqualTo(Propagation.MANDATORY);
        }
    }
}
