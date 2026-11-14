package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.store.common.constants.Constants;
import lombok.Getter;

/**
 * The server stayed saturated with imports (all slots taken for the whole wait, BR-CAT-13): a capacity problem of this
 * instance, not of the client → 503; the client should retry after {@link #retryAfterSeconds}.
 */
@Getter
public class ImportBusyException extends RuntimeException {

    private final int retryAfterSeconds;

    public ImportBusyException(int retryAfterSeconds) {
        super(String.format(Constants.ERROR_MSG_IMPORT_BUSY, retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
