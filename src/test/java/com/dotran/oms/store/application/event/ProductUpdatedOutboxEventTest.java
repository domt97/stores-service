package com.dotran.oms.store.application.event;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.event.ProductCreatedEvent;
import com.dotran.oms.store.domain.event.ProductUpdatedEvent;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** ProductUpdated payload against oms-workspace contracts/events/ProductUpdated.schema.json. */
@DisplayName("OutboxEventHelper — ProductUpdated")
class ProductUpdatedOutboxEventTest {

    private final OutboxEventHelper helper = new OutboxEventHelper();
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final ProductId productId = ProductId.newProductId();
    private final ProductSkuId keptId = ProductSkuId.newProductSkuId();

    private StoreProduct product(UUID categoryId) {
        ProductSku kept = ProductSku.builder().id(keptId).productId(productId).sku(SKU.of("MT-L")).name("Large")
                .price(new BigDecimal("45000")).currency("VND").status(ProductSkuStatus.INACTIVE).build();
        ProductSku removed = ProductSku.builder().id(ProductSkuId.newProductSkuId()).productId(productId).sku(SKU.of("MT-S"))
                .price(new BigDecimal("30000")).currency("VND").status(ProductSkuStatus.ACTIVE).removedAt(Instant.now()).build();
        return StoreProduct.builder().id(productId).storeId(StoreId.newStoreId()).name("Milk Tea").status(ProductStatus.ACTIVE)
                .categoryId(categoryId != null ? CategoryId.of(categoryId) : null)
                .skus(new ArrayList<>(List.of(kept, removed))).images(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("builds a PENDING ProductUpdated outbox event keyed by the product, with the given version and merchant")
    void buildsEvent() {
        StoreProduct product = product(null);

        OutboxEvent event = helper.createProductUpdatedOutboxEvent(merchantId, product, 7L);

        assertThat(event.getEventType()).isEqualTo("ProductUpdated");
        assertThat(event.getAggregateType()).isEqualTo("Product");
        assertThat(event.getAggregateId()).isEqualTo(productId.getValue());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        ProductUpdatedEvent payload = (ProductUpdatedEvent) event.getPayload();
        assertThat(payload.getEventId()).isEqualTo(event.getId().getValue());
        assertThat(payload.getVersion()).isEqualTo(7L);
        assertThat(payload.getMerchantId()).isEqualTo(merchantId.getValue());
        assertThat(payload.getStoreId()).isEqualTo(product.getStoreId().getValue());
    }

    @Test
    @DisplayName("JSON matches the contract: plain-string sku, statuses as names, removed SKUs left out, categoryId null")
    void jsonMatchesContract() {
        JsonNode json = objectMapper.valueToTree(helper.createProductUpdatedOutboxEvent(merchantId, product(null), 7L).getPayload());

        assertThat(json.get("eventType").asText()).isEqualTo("ProductUpdated");
        assertThat(json.get("version").asLong()).isEqualTo(7L);
        assertThat(json.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(json.get("name").asText()).isEqualTo("Milk Tea");
        assertThat(json.has("categoryId")).isTrue();
        assertThat(json.get("categoryId").isNull()).isTrue();
        assertThat(json.get("occurredAt").isTextual()).isTrue();
        assertThat(json.get("skus")).hasSize(1);
        JsonNode sku = json.get("skus").get(0);
        assertThat(sku.get("id").asText()).isEqualTo(keptId.getValue().toString());
        assertThat(sku.get("sku").isTextual()).isTrue();
        assertThat(sku.get("sku").asText()).isEqualTo("MT-L");
        assertThat(sku.get("status").asText()).isEqualTo("INACTIVE");
        assertThat(sku.get("price").isNumber()).isTrue();
        assertThat(sku.get("currency").asText()).isEqualTo("VND");
    }

    @Test
    @DisplayName("ProductCreated carries version 0")
    void productCreated_versionZero() {
        StoreProduct product = product(UUID.randomUUID());

        ProductCreatedEvent payload = (ProductCreatedEvent) helper.createProductCreatedOutboxEvent(merchantId, product).getPayload();

        assertThat(payload.getVersion()).isZero();
        assertThat(objectMapper.valueToTree(payload).get("version").asLong()).isZero();
        assertThat(payload.getSkus()).extracting(SKU::getValue).containsExactly("MT-L");
    }
}
