package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;

import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StoreProduct extends AggregateRoot<ProductId> {

    // BR-CAT-11
    public static final int MAX_SKUS = 100;
    public static final int MAX_IMAGES = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;

    private static final Comparator<ProductReplacement.ImageSpec> IMAGE_ORDER = Comparator
            .comparing(ProductReplacement.ImageSpec::displayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ProductReplacement.ImageSpec::imageUrl, Comparator.nullsLast(Comparator.naturalOrder()));

    private StoreId storeId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private CategoryId categoryId;
    private ProductStatus status;
    private List<ProductSku> skus;
    private List<ProductImage> images;
    /** Optimistic version (0 when created); only the guarded version bump in persistence changes it. */
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public void initState() {
        // Own mutable copies: callers may pass immutable lists (e.g. Stream.toList() in the mapper).
        this.skus = this.skus != null ? new ArrayList<>(this.skus) : new ArrayList<>();
        this.images = this.images != null ? new ArrayList<>(this.images) : new ArrayList<>();
        checkLimits(this.description, this.skus.size(), this.images.size());
        checkNoDuplicateCodes(this.skus.stream().map(ProductSku::getSku).toList());
        this.skus.forEach(ProductSku::normaliseForSale);
        this.status = ProductStatus.ACTIVE;
        checkActiveHasActiveSku(this.status, this.skus.stream().map(ProductSku::getStatus).toList());
        this.createdAt = this.updatedAt = Instant.now();
    }

    /** SKUs that are part of the product; removed SKUs are left out (see {@link #getAllSkusIncludingRemoved()}). */
    public List<ProductSku> getSkus() {
        return skus == null ? null : skus.stream().filter(sku -> !sku.isRemoved()).toList();
    }

    /** Including removed SKUs, whose codes stay reserved and which can be revived. For update and persistence only. */
    public List<ProductSku> getAllSkusIncludingRemoved() {
        return skus;
    }

    /**
     * Full replace (update-product). Validates the requested state against the rules (BR-CAT-08..11, SKU price rule),
     * applies it to this product and returns what changed; an empty change set means the request equals the current
     * state and nothing was changed. SKUs are matched by code; codes left out are removed (soft), codes sent again are
     * revived. Every SKU in the request passes the SKU price rule ({@link ProductSku#normaliseForSale()}), so a stored
     * SKU that breaks it must be corrected when the product is edited: the ProductUpdated contract requires valid prices.
     */
    public ProductChangeSet replace(ProductReplacement replacement, Instant now) {
        List<ProductSkuSpec> specs = replacement.skus() == null ? List.of() : replacement.skus();
        List<ProductReplacement.ImageSpec> imageSpecs = replacement.images() == null ? List.of() : replacement.images();

        if (replacement.name() == null || replacement.name().isBlank()) {
            throw new ValidationException(Constants.ERROR_MSG_PRODUCT_NAME_REQUIRED);
        }
        checkLimits(replacement.description(), specs.size(), imageSpecs.size());
        if (specs.stream().anyMatch(spec -> spec.status() == null)) {
            throw new ValidationException(Constants.ERROR_MSG_SKU_STATUS_REQUIRED);
        }
        if (specs.isEmpty()) {
            throw new ValidationException(Constants.ERROR_MSG_PRODUCT_NEEDS_SKU);
        }
        checkNoDuplicateCodes(specs.stream().map(ProductSkuSpec::sku).toList());
        checkStatusChange(this.status, replacement.status());
        checkActiveHasActiveSku(replacement.status(), specs.stream().map(ProductSkuSpec::status).toList());

        Map<String, ProductSku> storedByCode = new LinkedHashMap<>();
        for (ProductSku stored : skus) {
            storedByCode.put(stored.getSku().getValue(), stored);
        }

        List<ProductSku> added = new ArrayList<>();
        List<ProductSku> updated = new ArrayList<>();
        Set<String> requestedCodes = new HashSet<>();
        for (ProductSkuSpec spec : specs) {
            String code = spec.sku().getValue();
            requestedCodes.add(code);
            ProductSku stored = storedByCode.get(code);
            checkSpecId(spec, stored);
            ProductSku candidate = ProductSku.fromSpec(getId(), spec);
            if (stored == null) {
                added.add(candidate);
            } else if (stored.isRemoved() || !stored.sameValuesAs(candidate)) {
                updated.add(stored);
                stored.applyValuesOf(candidate, now);
            }
        }

        List<ProductSku> removed = new ArrayList<>();
        for (ProductSku stored : skus) {
            if (!stored.isRemoved() && !requestedCodes.contains(stored.getSku().getValue())) {
                removed.add(stored);
                stored.remove(now);
            }
        }

        boolean productFieldsChanged = !Objects.equals(name, replacement.name())
                || !Objects.equals(description, replacement.description())
                || !Objects.equals(thumbnailUrl, replacement.thumbnailUrl())
                || !Objects.equals(categoryId, replacement.categoryId())
                || status != replacement.status();
        List<ProductReplacement.ImageSpec> requestedImages = imageSpecs.stream().sorted(IMAGE_ORDER).toList();
        boolean imagesChanged = !currentImageSpecs().equals(requestedImages);

        ProductChangeSet changes = new ProductChangeSet(productFieldsChanged, imagesChanged, added, updated, removed);
        if (changes.isEmpty()) {
            return changes;
        }

        this.name = replacement.name();
        this.description = replacement.description();
        this.thumbnailUrl = replacement.thumbnailUrl();
        this.categoryId = replacement.categoryId();
        this.status = replacement.status();
        if (imagesChanged) {
            this.images = new ArrayList<>(requestedImages.stream()
                    .map(image -> ProductImage.builder()
                            .productId(getId())
                            .imageUrl(image.imageUrl())
                            .displayOrder(image.displayOrder())
                            .createdAt(now)
                            .build())
                    .toList());
        }
        added.forEach(sku -> sku.attachTo(getId(), now));
        this.skus = new ArrayList<>(skus);
        this.skus.addAll(added);
        this.updatedAt = now;
        return changes;
    }

    private List<ProductReplacement.ImageSpec> currentImageSpecs() {
        return (images == null ? List.<ProductImage>of() : images).stream()
                .map(image -> new ProductReplacement.ImageSpec(image.getImageUrl(), image.getDisplayOrder()))
                .sorted(IMAGE_ORDER)
                .toList();
    }

    private static void checkLimits(String description, int skuCount, int imageCount) {
        if (skuCount > MAX_SKUS || imageCount > MAX_IMAGES
                || (description != null && description.length() > MAX_DESCRIPTION_LENGTH)) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_PRODUCT_LIMITS,
                    MAX_SKUS, MAX_IMAGES, MAX_DESCRIPTION_LENGTH));
        }
    }

    private static void checkNoDuplicateCodes(List<SKU> codes) {
        Set<String> seen = new HashSet<>();
        for (SKU code : codes) {
            if (code == null || code.getValue() == null || code.getValue().isBlank()) {
                throw new ValidationException(Constants.ERROR_MSG_SKU_CODE_REQUIRED);
            }
            if (!seen.add(code.getValue())) {
                throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_CODE_DUPLICATE, code.getValue()));
            }
        }
    }

    /** BR-CAT-10: DRAFT → ACTIVE / INACTIVE, ACTIVE ↔ INACTIVE, never back to DRAFT. */
    private static void checkStatusChange(ProductStatus current, ProductStatus requested) {
        if (requested == null) {
            throw new ValidationException(Constants.ERROR_MSG_PRODUCT_STATUS_REQUIRED);
        }
        if (requested == ProductStatus.DRAFT && current != ProductStatus.DRAFT) {
            throw new ProductRuleViolationException(String.format(Constants.ERROR_MSG_PRODUCT_STATUS_CHANGE, current, requested));
        }
    }

    /** BR-CAT-10: an ACTIVE product needs at least one ACTIVE SKU. */
    private static void checkActiveHasActiveSku(ProductStatus status, List<ProductSkuStatus> skuStatuses) {
        if (status == ProductStatus.ACTIVE && !skuStatuses.contains(ProductSkuStatus.ACTIVE)) {
            throw new ProductRuleViolationException(Constants.ERROR_MSG_PRODUCT_ACTIVE_NEEDS_ACTIVE_SKU);
        }
    }

    private static void checkSpecId(ProductSkuSpec spec, ProductSku stored) {
        ProductSkuId requestedId = spec.id();
        if (requestedId == null) {
            return;
        }
        if (stored == null || !requestedId.getValue().equals(stored.getId().getValue())) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_SKU_ID_MISMATCH, spec.sku().getValue()));
        }
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void addSku(ProductSku sku) {
        checkLimits(this.description, getSkus().size() + 1, this.images == null ? 0 : this.images.size());
        List<SKU> codes = new ArrayList<>(this.skus.stream().map(ProductSku::getSku).toList());
        codes.add(sku.getSku());
        checkNoDuplicateCodes(codes);
        sku.normaliseForSale();
        this.skus.add(sku);
        this.updatedAt = Instant.now();
    }

    public void addImage(ProductImage image) {
        this.images.add(image);
        this.updatedAt = Instant.now();
    }

    public BigDecimal getMinPrice() {
        return Optional.ofNullable(getSkus())
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMaxPrice() {
        return Optional.ofNullable(getSkus())
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public String getCurrency() {
        return Optional.ofNullable(getSkus())
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getCurrency)
                .findFirst()
                .orElse(null);
    }

    public int getSkuCount() {
        return Optional.ofNullable(getSkus())
                .orElseGet(List::of)
                .size();
    }

    public List<SKU> getListOfSKUs() {
        return Optional.ofNullable(getSkus())
                .orElseGet(List::of)
                .stream()
                .map(ProductSku::getSku)
                .toList();
    }

}
