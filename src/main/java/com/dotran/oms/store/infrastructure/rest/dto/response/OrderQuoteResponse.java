package com.dotran.oms.store.infrastructure.rest.dto.response;

import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.enums.StoreStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Internal order quote (ADR 0007). Advisory, not a reservation: statuses and currencies are raw, and order-service
 * applies the placement rules. {@code localDate} is null when the store's time zone cannot be read.
 */
public record OrderQuoteResponse(StoreSnapshot store, MerchantStatus merchantStatus, Instant now, LocalDate localDate,
                                 boolean open, List<OpenReason> reasons, List<AvailabilityType> closedWindowTypes,
                                 List<SkuQuote> skus) {

    public record StoreSnapshot(UUID id, String code, String name, StoreStatus status, AddressSnapshot address,
                                String timeZone, String currency, boolean autoAcceptOrder, boolean allowPreOrder,
                                Integer maxOrdersPerDay, Integer preparationTimeMinutes) {
    }

    /** Every field may be null; the whole address is null when the store has none. */
    public record AddressSnapshot(String phone, String addressLine1, String addressLine2, String ward, String district,
                                  String province, String city, String country, String postalCode) {
    }

    /** One per distinct requested code, in request order; product fields null when {@code found = false}. */
    public record SkuQuote(String sku, boolean found, UUID productId, String productName, String skuName,
                           BigDecimal price, String currency, ProductSkuStatus status, ProductStatus productStatus) {
    }
}
