package com.codestorykh.money.dto;

import java.math.BigDecimal;

/**
 * Request for strategy-based pricing (P2P_TRANSFER, BILL_PAYMENT, ...).
 */
public record PricingRequestDto(
        String serviceType,
        String senderAccountId,
        String receiverAccountId,
        String sourceCurrency,
        String targetCurrency,
        String txnCurrency,
        BigDecimal txnAmount,
        String customerTier
) {
    public CalculationContext toContext() {
        return new CalculationContext(
                serviceType,
                senderAccountId,
                receiverAccountId,
                sourceCurrency,
                targetCurrency,
                txnCurrency,
                txnAmount,
                customerTier
        );
    }
}
