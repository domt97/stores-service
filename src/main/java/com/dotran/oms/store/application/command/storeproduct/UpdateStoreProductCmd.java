package com.dotran.oms.store.application.command.storeproduct;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.domain.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Full replace of a product (update-product). {@code version} is the version the client last read. */
@Data
@Builder
public class UpdateStoreProductCmd {

    private MerchantId merchantId;
    private StoreId storeId;
    private ProductId productId;
    private Long version;

    private String name;
    private String description;
    private String thumbnailUrl;
    private CategoryId categoryId;
    private ProductStatus status;
    private List<UpdateProductSkuCmd> skus;
    private List<ProductImageCmd> images;
}
