package com.dotran.example.store.domain.valueobject;

import com.dotran.example.store.domain.exception.MoneyMismatchException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@EqualsAndHashCode
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        validateCurrency(other);

        return new Money(
                this.amount.add(other.amount),
                currency
        );
    }

    public Money subtract(Money other) {
        validateCurrency(other);

        return new Money(
                this.amount.subtract(other.amount),
                currency
        );
    }

    public Money multiply(int quantity) {

        return new Money(
                this.amount.multiply(
                        BigDecimal.valueOf(quantity)
                ),
                currency
        );
    }

    public Money multiply(BigDecimal multiplier) {

        return new Money(
                this.amount.multiply(multiplier),
                currency
        );
    }

    private void validateCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new MoneyMismatchException();
        }
    }

}
