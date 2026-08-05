package com.codestorykh.money.dto;

import com.codestorykh.money.domain.Money;

import java.math.BigDecimal;

public record CalculationContext(
    String serviceType,            // e.g., "BILL_PAYMENT", "P2P_TRANSFER", "MERCHANT_QR"
    String senderAccountId,
    String receiverAccountId,
    String sourceCurrency,         // Sender Account DB Currency (e.g., "USD")
    String targetCurrency,         // Receiver Account DB Currency (e.g., "KHR")
    String txnCurrency,            // Instruction currency (e.g., "KHR" for a bill)
    BigDecimal txnAmount,          // Amount requested in txnCurrency
    String customerTier            // "RETAIL", "VIP", "MERCHANT"
) {
    /**
     * Source-side money used by pricing strategies when the instruction is quoted in source currency.
     */
    public Money sourceMoney() {
        return new Money(txnAmount, sourceCurrency);
    }
}
