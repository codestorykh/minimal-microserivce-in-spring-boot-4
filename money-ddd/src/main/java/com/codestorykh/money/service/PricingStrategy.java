package com.codestorykh.money.service;

import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;

public interface PricingStrategy {

    /**
     * Identifies which banking service this strategy belongs to
     * e.g., "P2P_TRANSFER", "BILL_PAYMENT", "MERCHANT_QR"
     */
    String getServiceType();

    /**
     * Calculates FX Rate, Fees, and Commissions for the given service.
     */
    CalculationResult calculate(CalculationContext context);
}