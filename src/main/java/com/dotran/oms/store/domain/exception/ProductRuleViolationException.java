package com.dotran.oms.store.domain.exception;

import com.dotran.oms.core.exception.BusinessException;

/** A product state the business rules forbid, e.g. a status change (BR-CAT-10). Mapped to 422. */
public class ProductRuleViolationException extends BusinessException {

    public ProductRuleViolationException(String message) {
        super(message);
    }
}
