package com.dotran.oms.store.common.id;

import com.dotran.oms.core.domain.id.BaseId;
import lombok.Getter;

@Getter
public class ProductImageId extends BaseId<Long> {

    public ProductImageId(Long value) {
        super(value);
    }

    public static ProductImageId of(Long value) {
        return new ProductImageId(value);
    }
}
