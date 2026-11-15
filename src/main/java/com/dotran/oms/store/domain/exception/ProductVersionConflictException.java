package com.dotran.oms.store.domain.exception;

/** The product changed since the client read it (stale version, or a concurrent update). Mapped to 409. */
public class ProductVersionConflictException extends RuntimeException {

    public ProductVersionConflictException(String message) {
        super(message);
    }
}
