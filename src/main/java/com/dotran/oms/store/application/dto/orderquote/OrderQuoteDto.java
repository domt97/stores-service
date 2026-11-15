package com.dotran.oms.store.application.dto.orderquote;

import com.dotran.oms.core.valueobject.Address;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.valueobject.OpenStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What order-service needs from the store at placement and for a delivery quote (ADR 0007). Read-only, advisory. */
@Builder
public record OrderQuoteDto(UUID storeId, String code, String name, StoreStatus status, Address address,
                            String timeZone, String currency, boolean autoAcceptOrder, boolean allowPreOrder,
                            Integer maxOrdersPerDay, Integer preparationTimeMinutes, MerchantStatus merchantStatus,
                            Instant now, LocalDate localDate, OpenStatus openStatus, List<SkuQuote> skus) {

    /** One requested SKU, in request order; {@code found = false} (product fields null) when not sellable data exists. */
    public record SkuQuote(String sku, boolean found, UUID productId, String productName, String skuName,
                           BigDecimal price, String currency, ProductSkuStatus status, ProductStatus productStatus) {

        public static SkuQuote notFound(String sku) {
            return new SkuQuote(sku, false, null, null, null, null, null, null, null);
        }

        public static SkuQuote of(SkuQuoteRow row) {
            return new SkuQuote(row.sku(), true, row.productId(), row.productName(), row.skuName(), row.price(),
                    row.currency(), row.status(), row.productStatus());
        }
    }
}
