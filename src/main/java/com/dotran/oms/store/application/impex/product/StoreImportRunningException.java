package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.store.common.constants.Constants;

/** An import for this store is already running (one import per store at a time, BR-CAT-13) → 409. */
public class StoreImportRunningException extends RuntimeException {

    public StoreImportRunningException() {
        super(Constants.ERROR_MSG_IMPORT_STORE_RUNNING);
    }
}
