package com.codestorykh.money.repository;

import com.codestorykh.money.domain.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    Optional<FxRate> findBySourceCurrencyIgnoreCaseAndTargetCurrencyIgnoreCase(
            String sourceCurrency,
            String targetCurrency
    );

    default BigDecimal getRate(String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            return BigDecimal.ONE;
        }
        return findBySourceCurrencyIgnoreCaseAndTargetCurrencyIgnoreCase(sourceCurrency, targetCurrency)
                .map(FxRate::getRate)
                .orElseThrow(() -> new IllegalArgumentException(
                        "UNSUPPORTED_FX_PAIR: " + sourceCurrency + "->" + targetCurrency
                ));
    }
}
