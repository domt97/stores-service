package com.dotran.oms.store.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Resource
    public static final String MERCHANT = "Merchant";
    public static final String STORE = "Store";
    public static final String PRODUCT = "Product";

    // Platform currency (BR-MER-01: one market, VND, no decimals). SKUs may still carry other currencies.
    public static final String CURRENCY_VND = "VND";

    // Common error
    public static final String ERROR_MSG_NOT_FOUND = "Resource not found";

    // Store business error
    public static final String ERROR_MSG_MERCHANT_NOT_FOUND = "Merchant not found";
    public static final String ERROR_MSG_MERCHANT_CODE_ALREADY_EXISTS = "Merchant code already exists";
    public static final String ERROR_MSG_STORE_NOT_FOUND = "Store not found";
    public static final String ERROR_MSG_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_MSG_SKU_CURRENCY_INVALID = "Currency must be a 3-letter code (SKU %s)";
    public static final String ERROR_MSG_SKU_PRICE_INVALID = "Price must be zero or more, below 10^16, with at most 2 decimals (SKU %s)";
    public static final String ERROR_MSG_SKU_PRICE_NOT_WHOLE_VND = "A VND price must be a whole number (SKU %s)";
    public static final String ERROR_MSG_PRODUCT_LIMITS = "A product has at most %d SKUs, %d images and a description of %d characters";
    public static final String ERROR_MSG_PRODUCT_NEEDS_SKU = "A product needs at least one SKU";
    public static final String ERROR_MSG_PRODUCT_NAME_REQUIRED = "Product name is required";
    public static final String ERROR_MSG_SKU_STATUS_REQUIRED = "A SKU status is required";
    public static final String ERROR_MSG_SKU_CODE_CONCURRENT = "Another product of the store is being saved with the same SKU code at the same time; try again";
    public static final String ERROR_MSG_PRODUCT_STATUS_REQUIRED = "Product status is required";
    public static final String ERROR_MSG_PRODUCT_STATUS_CHANGE = "A product cannot go from %s to %s";
    public static final String ERROR_MSG_PRODUCT_ACTIVE_NEEDS_ACTIVE_SKU = "An ACTIVE product needs at least one ACTIVE SKU";
    public static final String ERROR_MSG_SKU_CODE_REQUIRED = "A SKU code is required";
    public static final String ERROR_MSG_SKU_CODE_DUPLICATE = "SKU code %s appears more than once";
    public static final String ERROR_MSG_SKU_ID_MISMATCH = "The id sent for SKU %s is not the id of that SKU";
    public static final String ERROR_MSG_SKU_CODE_IN_USE = "SKU code(s) already used by another product of the store: %s";
    public static final String ERROR_MSG_PRODUCT_VERSION_CONFLICT = "The product was changed by someone else (current version %d); reload it and try again";
    public static final String ERROR_MSG_PRODUCT_CONCURRENT_UPDATE = "The product was changed by someone else at the same time; reload it and try again";
    public static final String ERROR_MSG_IMPORT_REFERENCE_REQUIRED = "Every row of the %s sheet needs a product reference";
    public static final String ERROR_MSG_IMPORT_REFERENCE_DUPLICATE = "Product reference %s appears more than once in the Product sheet";
    public static final String ERROR_MSG_COLLECTION_NOT_FOUND = "Collection not found";
    public static final String ERROR_MSG_INVALID_SORT_FIELD = "sortBy must be one of createdAt, updatedAt, name, id";
    public static final String ERROR_MSG_INVALID_SORT_DIRECTION = "direction must be ASC or DESC";
    public static final String ERROR_MSG_INVALID_PAGE = "pageNumber must be 0 or more and pageSize 1 or more";
    public static final String ERROR_MSG_INVALID_PAGE_MAX = "pageNumber must be 0 or more and pageSize between 1 and %d";
    public static final String ERROR_MSG_PRODUCT_ID_REQUIRED = "Product id must not be null";
    public static final String ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND = "Store availability not found";
    public static final String ERROR_MSG_STORE_ALREADY_CLOSED = "Store is already closed";
    public static final String ERROR_MSG_STORE_IS_NOT_CLOSED = "Store is not closed";
    public static final String ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG = "Missing business hour config for %s";

}
