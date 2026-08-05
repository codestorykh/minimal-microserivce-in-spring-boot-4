package com.codestorykh.money.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("Amount and Currency cannot be null");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    // Explicit Conversion Method - The ONLY way currency changes
    public Money convert(BigDecimal fxRate, String targetCurrency) {
        BigDecimal convertedAmount = this.amount.multiply(fxRate);
        // KHR doesn't use decimals, USD uses 2 decimal places
        int scale = "KHR".equalsIgnoreCase(targetCurrency) ? 0 : 2;
        return new Money(convertedAmount.setScale(scale, RoundingMode.HALF_UP), targetCurrency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new IllegalArgumentException("Factor cannot be null");
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money add(Money other) {
        verifySameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        verifySameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    private void verifySameCurrency(Money other) {
        if (!this.currency.equalsIgnoreCase(other.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Mismatch Currency Exception: Cannot operate %s on %s", this.currency, other.getCurrency())
            );
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}