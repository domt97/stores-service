package com.dotran.example.store.common.domain.valueobject;

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
