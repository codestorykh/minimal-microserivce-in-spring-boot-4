package com.codestorykh.money.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PricingStrategyFactory {

    private final Map<String, PricingStrategy> strategyMap;

    public PricingStrategyFactory(List<PricingStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PricingStrategy::getServiceType, Function.identity()));
    }

    public PricingStrategy getStrategy(String serviceType) {
        PricingStrategy strategy = strategyMap.get(serviceType.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("UNSUPPORTED_SERVICE_TYPE: No pricing strategy found for " + serviceType);
        }
        return strategy;
    }
}
