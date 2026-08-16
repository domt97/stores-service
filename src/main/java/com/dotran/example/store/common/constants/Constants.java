package com.dotran.example.store.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Resource
    public static final String TENANT = "Tenant";
    public static final String STORE = "Store";
    public static final String PRODUCT = "Product";

    // Common error
    public static final String ERROR_MSG_NOT_FOUND = "Resource not found";

    // Store business error
    public static final String ERROR_MSG_TENANT_NOT_FOUND = "Tenant not found";
    public static final String ERROR_MSG_STORE_NOT_FOUND = "Store not found";
    public static final String ERROR_MSG_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND = "Store availability not found";
    public static final String ERROR_MSG_STORE_ALREADY_CLOSED = "Store is already closed";
    public static final String ERROR_MSG_STORE_IS_NOT_CLOSED = "Store is not closed";
    public static final String ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG = "Missing business hour config for %s";

}
