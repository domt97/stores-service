package com.dotran.oms.store.domain.exception;

/** A SKU code is already used by another product of the store, removed SKUs included (BR-CAT-02, BR-CAT-09). Mapped to 409. */
public class SkuCodeInUseException extends RuntimeException {

    public SkuCodeInUseException(String message) {
        super(message);
    }
}
