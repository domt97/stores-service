package com.dotran.oms.store.application.event;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.event.ProductCreatedEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventHelper {

    public OutboxEvent createProductCreatedOutboxEvent(TenantId tenantId, StoreProduct storeProduct) {
        EventId eventId = EventId.newEventId();
        Instant now = Instant.now();

        ProductCreatedEvent payload = ProductCreatedEvent.builder()
                .eventId(eventId.getValue())
                .occurredAt(now)
                .tenantId(tenantId.getValue())
                .storeId(storeProduct.getStoreId().getValue())
                .productId(storeProduct.getId().getValue())
                .skus(storeProduct.getListOfSKUs())
                .build();

        return OutboxEvent.builder()
                .id(eventId)
                .aggregateType("Product")
                .aggregateId(storeProduct.getId().getValue())
                .eventType("ProductCreated")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .build();
    }

    public List<OutboxEvent> createProductCreatedOutboxEvents(TenantId tenantId, List<StoreProduct> storeProducts) {
        return storeProducts.stream()
                .map(storeProduct -> createProductCreatedOutboxEvent(tenantId, storeProduct))
                .toList();
    }
}
