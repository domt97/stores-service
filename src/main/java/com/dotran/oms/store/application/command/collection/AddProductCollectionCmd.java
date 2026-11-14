package com.dotran.oms.store.application.command.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.common.id.StoreCollectionId;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AddProductCollectionCmd {

    private MerchantId merchantId;
    private StoreId storeId;
    private StoreCollectionId storeCollectionId;
    private List<ProductId> productIds;
}
