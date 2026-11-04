package com.dotran.oms.store.application.command.storeproduct;

import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class UpdateProductSkuCmd {

    /** Optional; when sent it must be the id of the stored SKU with this code. */
    private UUID id;
    private SKU sku;
    private String name;
    private BigDecimal price;
    private String currency;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private ProductSkuStatus status;
}
