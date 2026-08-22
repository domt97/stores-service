package com.dotran.example.store.common.domain.valueobject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Money {

    private final BigDecimal amount;
    private final String currency;
}
