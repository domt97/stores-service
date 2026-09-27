package com.dotran.oms.store.domain.exception;

public class MoneyMismatchException extends IllegalArgumentException {

    private static final String ERROR_MESSAGE = "Money mismatch";

    public MoneyMismatchException() {
        super(ERROR_MESSAGE);
    }
}
