package com.dotran.example.store.domain.exception;

import com.dotran.example.store.common.constants.Constants;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(String message) {
        super(message);
    }

    public StoreNotFoundException() {
        super(Constants.ERROR_MSG_STORE_NOT_FOUND);
    }
}
