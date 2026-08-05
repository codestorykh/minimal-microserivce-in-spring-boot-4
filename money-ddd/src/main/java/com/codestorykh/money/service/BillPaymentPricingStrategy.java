package com.codestorykh.money.service;

import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;
import com.codestorykh.money.dto.FeeBreakdown;
import com.codestorykh.money.repository.FxRateRepository;
import com.codestorykh.money.type.CurrencyBasis;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class BillPaymentPricingStrategy implements PricingStrategy {

    private final FxRateRepository fxRateRepository;

    public BillPaymentPricingStrategy(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    @Override
    public String getServiceType() {
        return "BILL_PAYMENT";
    }

    @Override
    public CalculationResult calculate(CalculationContext context) {
        BigDecimal rate = fxRateRepository.getRate(context.sourceCurrency(), context.targetCurrency());
        Money sourceMoney = context.sourceMoney();
        Money targetMoney = sourceMoney.convert(rate, context.targetCurrency());

        Money billerFeeKHR = Money.of(1000.00, context.targetCurrency());

        Money feeChargedToCustomer = billerFeeKHR;
        if (!context.sourceCurrency().equalsIgnoreCase(context.targetCurrency())) {
            BigDecimal usdFee = billerFeeKHR.getAmount().divide(rate, 2, RoundingMode.HALF_UP);
            feeChargedToCustomer = Money.of(usdFee, context.sourceCurrency());
        }

        return new CalculationResult(
                sourceMoney,
                targetMoney,
                rate,
                context.txnCurrency(),
                List.of(new FeeBreakdown("BILLER_FEE", CurrencyBasis.RECEIVER_CURRENCY, feeChargedToCustomer)),
                Money.of(0, context.sourceCurrency())
        );
    }
}
