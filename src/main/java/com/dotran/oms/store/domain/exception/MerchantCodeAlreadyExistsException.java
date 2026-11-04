package com.dotran.oms.store.domain.exception;

import com.dotran.oms.store.common.constants.Constants;

public class MerchantCodeAlreadyExistsException extends RuntimeException {

    public MerchantCodeAlreadyExistsException(String message) {
        super(message);
    }

    public MerchantCodeAlreadyExistsException() {
        super(Constants.ERROR_MSG_MERCHANT_CODE_ALREADY_EXISTS);
    }
}
