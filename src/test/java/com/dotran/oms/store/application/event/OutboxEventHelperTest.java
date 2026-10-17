package com.dotran.oms.store.application.event;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.event.ProductCreatedEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxEventHelper")
class OutboxEventHelperTest {

    private final OutboxEventHelper helper = new OutboxEventHelper();

    private StoreProduct buildProduct(StoreId storeId) {
        StoreProduct product = StoreProduct.builder()
                .id(ProductId.newProductId())
                .storeId(storeId)
                .name("Product")
                .skus(new ArrayList<>())
                .build();
        product.initState();
        return product;
    }

    @Test
    @DisplayName("creates an OutboxEvent with PENDING status")
    void createsEvent_withPendingStatus() {
        TenantId tenantId = TenantId.newTenantId();
        StoreProduct product = buildProduct(StoreId.newStoreId());

        OutboxEvent event = helper.createProductCreatedOutboxEvent(tenantId, product);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("creates an OutboxEvent with eventType ProductCreated")
    void createsEvent_withCorrectEventType() {
        OutboxEvent event = helper.createProductCreatedOutboxEvent(TenantId.newTenantId(), buildProduct(StoreId.newStoreId()));
        assertThat(event.getEventType()).isEqualTo("ProductCreated");
    }

    @Test
    @DisplayName("creates an OutboxEvent with aggregateType Product")
    void createsEvent_withCorrectAggregateType() {
        OutboxEvent event = helper.createProductCreatedOutboxEvent(TenantId.newTenantId(), buildProduct(StoreId.newStoreId()));
        assertThat(event.getAggregateType()).isEqualTo("Product");
    }

    @Test
    @DisplayName("creates an OutboxEvent with the product's ID as aggregateId")
    void createsEvent_withProductIdAsAggregateId() {
        StoreProduct product = buildProduct(StoreId.newStoreId());
        OutboxEvent event = helper.createProductCreatedOutboxEvent(TenantId.newTenantId(), product);
        assertThat(event.getAggregateId()).isEqualTo(product.getId().getValue());
    }

    @Test
    @DisplayName("payload contains the tenant ID and product ID")
    void payload_containsTenantAndProductId() {
        TenantId tenantId = TenantId.newTenantId();
        StoreProduct product = buildProduct(StoreId.newStoreId());

        OutboxEvent event = helper.createProductCreatedOutboxEvent(tenantId, product);
        ProductCreatedEvent payload = (ProductCreatedEvent) event.getPayload();

        assertThat(payload.getTenantId()).isEqualTo(tenantId.getValue());
        assertThat(payload.getProductId()).isEqualTo(product.getId().getValue());
    }

    @Test
    @DisplayName("createProductCreatedOutboxEvents creates one event per product")
    void createsOneEventPerProduct() {
        TenantId tenantId = TenantId.newTenantId();
        StoreId storeId = StoreId.newStoreId();
        List<StoreProduct> products = List.of(buildProduct(storeId), buildProduct(storeId));

        List<OutboxEvent> events = helper.createProductCreatedOutboxEvents(tenantId, products);

        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("retryCount is initialized to zero")
    void retryCount_isZero() {
        OutboxEvent event = helper.createProductCreatedOutboxEvent(TenantId.newTenantId(), buildProduct(StoreId.newStoreId()));
        assertThat(event.getRetryCount()).isZero();
    }
}
