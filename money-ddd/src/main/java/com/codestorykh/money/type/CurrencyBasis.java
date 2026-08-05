package com.codestorykh.money.type;

public enum CurrencyBasis {
    SENDER_CURRENCY,   // Fees/FX calculated using Source Amount (e.g., USD)
    RECEIVER_CURRENCY, // Fees/FX calculated using Target Amount (e.g., KHR)
    EQUIVALENT_USD     // Converted to base USD for global limit/tier evaluation
}