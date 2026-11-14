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
import java.util.Objects;
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
    /** Set when the SKU was removed from its product (soft remove, BR-CAT-09); null while the SKU is part of it. */
    private Instant removedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** A SKU as requested by an update-product: validated and normalised, not yet attached to a stored row. */
    public static ProductSku fromSpec(ProductId productId, ProductSkuSpec spec) {
        ProductSku candidate = ProductSku.builder()
                .productId(productId)
                .sku(spec.sku())
                .name(spec.name())
                .price(spec.price())
                .currency(spec.currency())
                .weight(spec.weight())
                .length(spec.length())
                .width(spec.width())
                .height(spec.height())
                .status(spec.status())
                .build();
        candidate.normaliseForSale();
        return candidate;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    /** True if name, price, currency, dimensions and status equal the other SKU's (amounts compared by value). */
    public boolean sameValuesAs(ProductSku other) {
        return Objects.equals(name, other.name)
                && sameAmount(price, other.price)
                && Objects.equals(currency, other.currency)
                && sameAmount(weight, other.weight)
                && sameAmount(length, other.length)
                && sameAmount(width, other.width)
                && sameAmount(height, other.height)
                && status == other.status;
    }

    /** Takes the values of a validated candidate; a removed SKU becomes part of its product again (revive). */
    void applyValuesOf(ProductSku candidate, Instant now) {
        this.name = candidate.name;
        this.price = candidate.price;
        this.currency = candidate.currency;
        this.weight = candidate.weight;
        this.length = candidate.length;
        this.width = candidate.width;
        this.height = candidate.height;
        this.status = candidate.status;
        this.removedAt = null;
        this.updatedAt = now;
    }

    void remove(Instant now) {
        this.removedAt = now;
        this.updatedAt = now;
    }

    void attachTo(ProductId productId, Instant now) {
        this.productId = productId;
        this.createdAt = this.updatedAt = now;
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

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
