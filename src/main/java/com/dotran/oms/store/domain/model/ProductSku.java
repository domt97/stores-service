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
import java.util.regex.Pattern;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductSku extends BaseDomain<ProductSkuId> {

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");
    // product_skus.price is NUMERIC(18,2): at most 16 integer digits.
    private static final BigDecimal PRICE_LIMIT = BigDecimal.TEN.pow(16);

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
     * A SKU price is a 3-letter currency code (stored upper-case) and an amount of zero or more with at most
     * 2 decimals (the column scale). A VND price must be a whole number of đồng (BR-MER-01).
     */
    public void normaliseForSale() {
        String skuCode = this.sku != null ? this.sku.getValue() : null;
        String currencyCode = this.currency != null ? this.currency.trim().toUpperCase(Locale.ROOT) : null;

        if (null == currencyCode || !CURRENCY_CODE.matcher(currencyCode).matches()) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_CURRENCY_INVALID, skuCode));
        }

        if (null == this.price || this.price.signum() < 0 || this.price.stripTrailingZeros().scale() > 2
                || this.price.compareTo(PRICE_LIMIT) >= 0) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_PRICE_INVALID, skuCode));
        }

        if (Constants.CURRENCY_VND.equals(currencyCode) && this.price.stripTrailingZeros().scale() > 0) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_PRICE_NOT_WHOLE_VND, skuCode));
        }

        this.currency = currencyCode;
    }
}
