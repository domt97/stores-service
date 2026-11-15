package com.dotran.oms.store.application.command.store;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import lombok.Builder;

/** Cancel one availability window of a store; merchant and store come from the request path. */
@Builder
public record CancelStoreAvailabilityCmd(MerchantId merchantId, StoreId storeId, StoreAvailabilityId storeAvailabilityId) {
}
