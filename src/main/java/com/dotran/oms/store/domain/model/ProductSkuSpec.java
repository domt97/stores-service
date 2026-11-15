package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;

import java.math.BigDecimal;

/**
 * One SKU of an update-product request. The code is the SKU's identity (BR-CAT-08); {@code id} is optional and, when
 * sent, must be the id of the stored SKU with that code.
 */
public record ProductSkuSpec(ProductSkuId id, SKU sku, String name, BigDecimal price, String currency,
                             BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal height,
                             ProductSkuStatus status) {
}
