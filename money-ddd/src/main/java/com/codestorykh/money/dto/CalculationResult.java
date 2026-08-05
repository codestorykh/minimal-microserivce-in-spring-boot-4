package com.codestorykh.money.dto;

import com.codestorykh.money.domain.Money;
import com.codestorykh.money.type.CurrencyBasis;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result payload containing the final calculated values for a transaction.
 */
public record CalculationResult(
        Money sourceMoney,
        Money targetMoney,
        BigDecimal appliedFxRate,
        String txnCurrency,
        List<FeeBreakdown> fees,
        Money agentCommission
) {
    public CalculationResult(
            Money sourceMoney,
            Money targetMoney,
            BigDecimal appliedFxRate,
            String txnCurrency,
            Money singleFee
    ) {
        this(
                sourceMoney,
                targetMoney,
                appliedFxRate,
                txnCurrency,
                List.of(new FeeBreakdown("SERVICE_FEE", CurrencyBasis.SENDER_CURRENCY, singleFee)),
                new Money(BigDecimal.ZERO, sourceMoney.getCurrency())
        );
    }
}
