package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

@Getter
public class ProductImageId extends BaseId<String> {

    public ProductImageId(String value) {
        super(value);
    }

    public static ProductImageId of(String value) {
        return new ProductImageId(value);
    }
}
