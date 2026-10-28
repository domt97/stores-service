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
