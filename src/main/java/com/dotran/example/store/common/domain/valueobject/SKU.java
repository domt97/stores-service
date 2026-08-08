package com.dotran.example.store.common.domain.valueobject;

import lombok.Getter;

@Getter
public class SKU extends BaseId<String> {

    public SKU(String value) {
        super(value);
    }

    public static SKU of(String value) {
        return new SKU(value);
    }
}
