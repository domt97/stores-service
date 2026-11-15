package com.dotran.oms.store.application.dto.orderquote;

import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** One SKU of the store that is not removed, with its product (read by one query, no aggregate loaded). */
public record SkuQuoteRow(String sku, UUID productId, String productName, String skuName, BigDecimal price,
                          String currency, ProductSkuStatus status, ProductStatus productStatus) {
}
