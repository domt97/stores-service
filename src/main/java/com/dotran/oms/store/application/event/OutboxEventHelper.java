package com.dotran.oms.store.application.event;

import com.dotran.oms.core.domain.id.EventId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.domain.enums.OutboxStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.event.ProductCreatedEvent;
import com.dotran.oms.store.domain.event.ProductUpdatedEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventHelper {

    public OutboxEvent createProductCreatedOutboxEvent(MerchantId merchantId, StoreProduct storeProduct) {
        EventId eventId = EventId.newEventId();
        Instant now = Instant.now();

        ProductCreatedEvent payload = ProductCreatedEvent.builder()
                .eventId(eventId.getValue())
                .occurredAt(now)
                .merchantId(merchantId.getValue())
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

    public List<OutboxEvent> createProductCreatedOutboxEvents(MerchantId merchantId, List<StoreProduct> storeProducts) {
        return storeProducts.stream()
                .map(storeProduct -> createProductCreatedOutboxEvent(merchantId, storeProduct))
                .toList();
    }

    /**
     * ProductUpdated after an update-product. {@code version} is the value the guarded bump wrote in this transaction;
     * {@code merchantId} comes from the loaded store, never from the request path.
     */
    public OutboxEvent createProductUpdatedOutboxEvent(MerchantId merchantId, StoreProduct storeProduct, long version) {
        EventId eventId = EventId.newEventId();
        Instant now = Instant.now();

        ProductUpdatedEvent payload = ProductUpdatedEvent.builder()
                .eventId(eventId.getValue())
                .occurredAt(now)
                .merchantId(merchantId.getValue())
                .storeId(storeProduct.getStoreId().getValue())
                .productId(storeProduct.getId().getValue())
                .version(version)
                .status(storeProduct.getStatus())
                .name(storeProduct.getName())
                .categoryId(storeProduct.getCategoryId() != null ? storeProduct.getCategoryId().getValue() : null)
                .skus(storeProduct.getSkus().stream()
                        .map(sku -> ProductUpdatedEvent.Sku.builder()
                                .id(sku.getId().getValue())
                                .sku(sku.getSku().getValue())
                                .name(sku.getName())
                                .status(sku.getStatus())
                                .price(sku.getPrice())
                                .currency(sku.getCurrency())
                                .build())
                        .toList())
                .build();

        return OutboxEvent.builder()
                .id(eventId)
                .aggregateType("Product")
                .aggregateId(storeProduct.getId().getValue())
                .eventType("ProductUpdated")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .build();
    }
}
