package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.BaseDomain;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductSku extends BaseDomain<ProductSkuId> {

    private ProductId productId;
    private SKU sku;
    private String name;
    private BigDecimal price;
    private String currency;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private ProductSkuStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public void init() {
        this.status = ProductSkuStatus.ACTIVE;
        this.createdAt = this.updatedAt = Instant.now();
    }

    /**
     * BR-MER-01: a SKU is sold in VND, in whole đồng, never below zero.
     * Normalises the currency code (any case, surrounding spaces) to {@code VND}.
     */
    public void normaliseForSale() {
        String skuCode = this.sku != null ? this.sku.getValue() : null;

        if (null == this.currency || !Constants.CURRENCY_VND.equals(this.currency.trim().toUpperCase(Locale.ROOT))) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_CURRENCY_NOT_SUPPORTED, skuCode));
        }

        if (null == this.price || this.price.signum() < 0 || this.price.stripTrailingZeros().scale() > 0) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_PRICE_INVALID, skuCode));
        }

        this.currency = Constants.CURRENCY_VND;
    }
}
