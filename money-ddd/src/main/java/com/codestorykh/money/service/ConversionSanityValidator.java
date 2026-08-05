package com.codestorykh.money.service;

import com.codestorykh.money.dto.TransactionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ConversionSanityValidator implements TransactionValidator {

    @Override
    public void validate(TransactionContext context) {
        // Only run for cross-currency transfers
        if (context.sourceMoney().getCurrency().equalsIgnoreCase(context.targetMoney().getCurrency())) {
            return;
        }

        // Reconstruct Source USD from Target KHR using the FX rate
        BigDecimal expectedSourceAmt = context.targetMoney().getAmount()
            .divide(BigDecimal.valueOf(context.appliedFxRate()), 2, RoundingMode.HALF_UP);

        BigDecimal actualSourceAmt = context.sourceMoney().getAmount();
        BigDecimal difference = expectedSourceAmt.subtract(actualSourceAmt).abs();

        // If the reconstructed amount differs by more than $0.05, block the transaction
        if (difference.compareTo(new BigDecimal("0.05")) > 0) {
            throw new IllegalStateException(String.format(
                "CRITICAL_CALCULATION_CORRUPTION: Conversion mismatch! Source: %s, Target: %s, Applied Rate: %f",
                context.sourceMoney(), context.targetMoney(), context.appliedFxRate()
            ));
        }
    }
}