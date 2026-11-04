package com.dotran.oms.store.application.command.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetListCollectionCmd {

    private MerchantId merchantId;
    private StoreId storeId;
}
