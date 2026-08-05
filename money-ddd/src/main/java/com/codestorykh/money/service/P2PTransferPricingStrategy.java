package com.codestorykh.money.service;

import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;
import com.codestorykh.money.dto.FeeBreakdown;
import com.codestorykh.money.repository.FeeConfigRepository;
import com.codestorykh.money.repository.FxRateRepository;
import com.codestorykh.money.type.CurrencyBasis;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class P2PTransferPricingStrategy implements PricingStrategy {

    private final FxRateRepository fxRateRepository;
    private final FeeConfigRepository feeConfigRepository;

    public P2PTransferPricingStrategy(FxRateRepository fxRateRepository, FeeConfigRepository feeConfigRepository) {
        this.fxRateRepository = fxRateRepository;
        this.feeConfigRepository = feeConfigRepository;
    }

    @Override
    public String getServiceType() {
        return "P2P_TRANSFER";
    }

    @Override
    public CalculationResult calculate(CalculationContext context) {
        BigDecimal rate = fxRateRepository.getRate(context.sourceCurrency(), context.targetCurrency());
        Money sourceMoney = context.sourceMoney();
        Money targetMoney = sourceMoney.convert(rate, context.targetCurrency());

        Money fee = calculateSenderBasedFee(sourceMoney);
        Money agentCommission = fee.multiply(new BigDecimal("0.20"));

        return new CalculationResult(
                sourceMoney,
                targetMoney,
                rate,
                context.txnCurrency(),
                List.of(new FeeBreakdown("SERVICE_FEE", CurrencyBasis.SENDER_CURRENCY, fee)),
                agentCommission
        );
    }

    private Money calculateSenderBasedFee(Money sourceMoney) {
        // Prefer DB fee config when present; otherwise use default tiered fee.
        var configs = feeConfigRepository.findByServiceTypeIgnoreCase(getServiceType());
        if (!configs.isEmpty() && configs.getFirst().getFlatAmount() != null) {
            return Money.of(configs.getFirst().getFlatAmount(), sourceMoney.getCurrency());
        }

        if (sourceMoney.getAmount().compareTo(new BigDecimal("100.00")) <= 0) {
            return Money.of(0.50, sourceMoney.getCurrency());
        }
        return Money.of(sourceMoney.getAmount().multiply(new BigDecimal("0.005")), sourceMoney.getCurrency());
    }
}
