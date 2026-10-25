package com.dotran.oms.store.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for the published ProductCreated payload, serialized the way the outbox adapter does
 * ({@code objectMapper.valueToTree}) with a real ObjectMapper close to Spring Boot's (unknown properties ignored, ISO dates).
 */
@DisplayName("ProductCreatedEvent JSON contract")
class ProductCreatedEventJsonTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    @DisplayName("payload carries merchantId (no tenantId), eventType and an ISO occurredAt")
    void payloadUsesMerchantId() {
        UUID merchantId = UUID.randomUUID();
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.parse("2026-10-25T10:00:00Z"))
                .merchantId(merchantId)
                .storeId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .build();

        JsonNode payload = objectMapper.valueToTree(event);

        assertThat(payload.get("merchantId").asText()).isEqualTo(merchantId.toString());
        assertThat(payload.has("tenantId")).isFalse();
        assertThat(payload.get("occurredAt").isTextual()).isTrue();
        assertThat(payload.get("eventType").asText()).isEqualTo("ProductCreated");
    }
}
