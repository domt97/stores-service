package com.dotran.oms.store.domain.exception;

import com.dotran.oms.store.common.constants.Constants;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(String message) {
        super(message);
    }

    public StoreNotFoundException() {
        super(Constants.ERROR_MSG_STORE_NOT_FOUND);
    }
}
