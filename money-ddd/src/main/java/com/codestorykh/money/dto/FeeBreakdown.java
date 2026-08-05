package com.codestorykh.money.dto;


import com.codestorykh.money.domain.Money;
import com.codestorykh.money.type.CurrencyBasis;

/**
 * Represents an individual fee component calculated by a pricing strategy.
 */
public record FeeBreakdown(
        String feeCode,       // e.g., "SERVICE_FEE", "BILLER_FEE", "CLEARING_FEE"
        CurrencyBasis currencyBasis,  // Indicates whether the fee is based on sender or receiver currency
        Money feeAmount    // Money object containing the fee value and fee currency
) {}