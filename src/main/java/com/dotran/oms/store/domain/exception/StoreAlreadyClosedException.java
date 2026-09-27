package com.dotran.oms.store.domain.exception;

import com.dotran.oms.store.common.constants.Constants;

public class StoreAlreadyClosedException extends RuntimeException {

    public StoreAlreadyClosedException(String message) {
        super(message);
    }

    public StoreAlreadyClosedException() {
        super(Constants.ERROR_MSG_STORE_ALREADY_CLOSED);
    }
}
