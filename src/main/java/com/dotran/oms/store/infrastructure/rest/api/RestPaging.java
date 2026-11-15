package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.constants.Constants;
import lombok.experimental.UtilityClass;

/** Page bounds checked in the controller so a bad value is a 400 with the usual error body, not a 500. */
@UtilityClass
class RestPaging {

    static void check(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 1) {
            throw new ValidationException(Constants.ERROR_MSG_INVALID_PAGE);
        }
    }

    static void check(int pageNumber, int pageSize, int maxPageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > maxPageSize) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_INVALID_PAGE_MAX, maxPageSize));
        }
    }
}
