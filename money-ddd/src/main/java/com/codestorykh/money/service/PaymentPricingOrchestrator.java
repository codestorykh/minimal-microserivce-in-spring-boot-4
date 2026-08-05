package com.codestorykh.money.service;

import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;
import org.springframework.stereotype.Service;

@Service
public class PaymentPricingOrchestrator {

    private final PricingStrategyFactory strategyFactory;

    public PaymentPricingOrchestrator(PricingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public CalculationResult calculatePricing(CalculationContext context) {
        PricingStrategy strategy = strategyFactory.getStrategy(context.serviceType());
        return strategy.calculate(context);
    }
}
