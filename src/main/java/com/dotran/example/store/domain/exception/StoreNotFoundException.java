package com.dotran.example.store.domain.exception;

import com.dotran.example.store.common.constants.ErrorMsgConstant;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(String message) {
        super(message);
    }

    public StoreNotFoundException() {
        super(ErrorMsgConstant.ERROR_MSG_STORE_NOT_FOUND);
    }
}
