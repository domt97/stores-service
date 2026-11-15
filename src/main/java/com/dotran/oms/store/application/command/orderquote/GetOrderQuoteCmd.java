package com.dotran.oms.store.application.command.orderquote;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import lombok.Builder;

import java.util.List;

/** Merchant and store from the request path; the SKU codes as sent (0..100, may contain duplicates or blanks). */
@Builder
public record GetOrderQuoteCmd(MerchantId merchantId, StoreId storeId, List<String> skus) {
}
