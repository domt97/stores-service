package com.dotran.example.store.domain.exception;

import com.dotran.example.store.common.constants.ErrorMsgConstant;

public class StoreAlreadyClosedException extends RuntimeException {

    public StoreAlreadyClosedException(String message) {
        super(message);
    }

    public StoreAlreadyClosedException() {
        super(ErrorMsgConstant.ERROR_MSG_STORE_ALREADY_CLOSED);
    }
}
