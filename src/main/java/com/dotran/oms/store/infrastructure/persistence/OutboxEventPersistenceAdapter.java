package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.logging.MdcPropagation;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inserts the outbox rows and deletes them again in the same transaction (oms-workspace open question 19): Debezium
 * publishes the INSERTs from the WAL, the table stays empty. Native SQL binds {@code id = eventId}, so the Kafka header
 * {@code id} equals the payload {@code eventId} (open question 45; JPA's generated id used to replace it).
 * {@link NamedParameterJdbcTemplate} joins the JPA transaction ({@code JpaTransactionManager} binds its connection to
 * the single DataSource), so the rows commit or roll back with the product change. Same pattern as inventory-service.
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPersistenceAdapter implements OutboxEventRepository {

    /**
     * {@code created_at = CURRENT_TIMESTAMP}: the session's local time in a TIMESTAMP column (before: UTC from the JVM);
     * irrelevant on a table that is always empty. {@code status} and {@code retry_count} keep their NOT NULL columns
     * filled until they are dropped.
     */
    private static final String INSERT = """
            INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count,
                                       created_at, correlation_context)
            VALUES (:id, :aggregateType, :aggregateId, :eventType, CAST(:payload AS jsonb), :status, :retryCount,
                    CURRENT_TIMESTAMP, CAST(:correlation AS jsonb))
            """;
    private static final String DELETE = "DELETE FROM outbox_events WHERE id IN (:ids)";
    /** One DELETE per chunk: an import can exceed any fixed cap ({@code app.import.max-products} is configurable); the driver allows 65 535 binds. */
    static final int DELETE_CHUNK = 1000;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final MdcPropagation mdcPropagation;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(OutboxEvent outboxEvent) {
        saveAll(List.of(outboxEvent));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveAll(List<OutboxEvent> outboxEvents) {
        if (outboxEvents.isEmpty()) {
            return;
        }
        // Taken on the writing thread (ADR 0004): every outbox write runs on the request thread.
        String correlation = toJson(mdcPropagation.capture());
        SqlParameterSource[] rows = outboxEvents.stream()
                .map(event -> new MapSqlParameterSource()
                        .addValue("id", event.getId().getValue())
                        .addValue("aggregateType", event.getAggregateType())
                        .addValue("aggregateId", event.getAggregateId())
                        .addValue("eventType", event.getEventType())
                        .addValue("payload", payloadJson(event.getPayload()))
                        .addValue("status", event.getStatus().name())
                        .addValue("retryCount", event.getRetryCount() == null ? 0 : event.getRetryCount())
                        .addValue("correlation", correlation))
                .toArray(SqlParameterSource[]::new);

        // Each count is 1; SUCCESS_NO_INFO only if the URL ever sets reWriteBatchedInserts.
        int[] inserted = jdbc.batchUpdate(INSERT, rows);
        for (int count : inserted) {
            if (count != 1 && count != Statement.SUCCESS_NO_INFO) {
                throw new IllegalStateException("Outbox insert wrote " + count + " rows");
            }
        }
        List<UUID> ids = outboxEvents.stream().map(event -> event.getId().getValue()).toList();
        int deleted = 0;
        for (int from = 0; from < ids.size(); from += DELETE_CHUNK) {
            List<UUID> chunk = ids.subList(from, Math.min(from + DELETE_CHUNK, ids.size()));
            deleted += jdbc.update(DELETE, Map.of("ids", chunk));
        }
        if (deleted != ids.size()) {
            throw new IllegalStateException("Outbox delete removed " + deleted + " of " + ids.size() + " rows");
        }

        outboxEvents.forEach(this::logQueued);
    }

    /**
     * Same stored JSON as before: through a tree first, which strips BigDecimal trailing zeros
     * ({@code 120000.00} → {@code 120000} after the jsonb cast), as the former JPA mapping of a JsonNode did.
     */
    private String payloadJson(Object payload) {
        try {
            return toJson(objectMapper.valueToTree(payload));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Outbox JSON serialization failed", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox JSON serialization failed", e);
        }
    }

    /**
     * Joins this request's logs (requestId in the MDC) with the consumer's: the consumer logs the same eventId.
     * "Queued", not "published": the record exists only once the transaction commits.
     */
    private void logQueued(OutboxEvent outboxEvent) {
        log.debug("Outbox event queued: eventType={} eventId={} aggregateId={}",
                outboxEvent.getEventType(), outboxEvent.getId().getValue(), outboxEvent.getAggregateId());
    }
}
