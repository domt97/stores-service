package com.dotran.oms.store.domain.exception;

import com.dotran.oms.store.common.constants.Constants;

/** Two requests changed the same collection's products at the same time and the database chose one to retry → 409. */
public class CollectionChangedConcurrentlyException extends RuntimeException {

    public CollectionChangedConcurrentlyException() {
        super(Constants.ERROR_MSG_COLLECTION_CONCURRENT_CHANGE);
    }
}
