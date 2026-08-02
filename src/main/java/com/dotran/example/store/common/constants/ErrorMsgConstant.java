package com.dotran.example.store.common.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ErrorMsgConstant {

    // Common error

    // Store business error
    public static final String ERROR_MSG_STORE_NOT_FOUND = "Store not found";
    public static final String ERROR_MSG_STORE_ALREADY_CLOSED = "Store is already closed";
    public static final String ERROR_MSG_STORE_IS_NOT_CLOSED = "Store is not closed";
    public static final String ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG = "Missing business hour config for %s";

}
