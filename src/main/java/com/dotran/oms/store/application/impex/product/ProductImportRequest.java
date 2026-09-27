package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductImportRequest {

    private final List<StoreProduct> products;
}
