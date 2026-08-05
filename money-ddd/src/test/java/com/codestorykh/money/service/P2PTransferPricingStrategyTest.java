package com.codestorykh.money.service;

import com.codestorykh.money.domain.FeeConfig;
import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;
import com.codestorykh.money.dto.FeeBreakdown;
import com.codestorykh.money.repository.FeeConfigRepository;
import com.codestorykh.money.repository.FxRateRepository;
import com.codestorykh.money.type.CurrencyBasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class P2PTransferPricingStrategyTest {

    @Mock
    private FxRateRepository fxRateRepository;

    @Mock
    private FeeConfigRepository feeConfigRepository;

    @InjectMocks
    private P2PTransferPricingStrategy strategy;

    @Test
    @DisplayName("Service type is P2P_TRANSFER")
    void getServiceType_returnsP2PTransfer() {
        assertEquals("P2P_TRANSFER", strategy.getServiceType());
    }

    @Test
    @DisplayName("Amount <= $100: flat $0.50 fee and 20% agent commission")
    void calculate_smallAmount_usesFlatFee() {
        when(fxRateRepository.getRate("USD", "KHR")).thenReturn(new BigDecimal("4040.00"));
        when(feeConfigRepository.findByServiceTypeIgnoreCase("P2P_TRANSFER")).thenReturn(List.of());

        CalculationContext context = context("USD", "KHR", "USD", new BigDecimal("100.00"));

        CalculationResult result = strategy.calculate(context);

        assertEquals(Money.of(100.00, "USD"), result.sourceMoney());
        assertEquals(Money.of(404000.00, "KHR"), result.targetMoney());
        assertEquals(0, new BigDecimal("4040.00").compareTo(result.appliedFxRate()));

        FeeBreakdown fee = result.fees().getFirst();
        assertEquals("SERVICE_FEE", fee.feeCode());
        assertEquals(CurrencyBasis.SENDER_CURRENCY, fee.currencyBasis());
        assertEquals(Money.of(0.50, "USD"), fee.feeAmount());
        assertEquals(Money.of(0.10, "USD"), result.agentCommission()); // 20% of 0.50

        verify(fxRateRepository).getRate("USD", "KHR");
        verify(feeConfigRepository).findByServiceTypeIgnoreCase("P2P_TRANSFER");
    }

    @Test
    @DisplayName("Amount > $100: uses 0.5% percentage fee")
    void calculate_largeAmount_usesPercentFee() {
        when(fxRateRepository.getRate("USD", "KHR")).thenReturn(new BigDecimal("4040.00"));
        when(feeConfigRepository.findByServiceTypeIgnoreCase("P2P_TRANSFER")).thenReturn(List.of());

        CalculationContext context = context("USD", "KHR", "USD", new BigDecimal("200.00"));

        CalculationResult result = strategy.calculate(context);

        assertEquals(Money.of(200.00, "USD"), result.sourceMoney());
        assertEquals(Money.of(808000.00, "KHR"), result.targetMoney());

        // 200 * 0.005 = 1.00
        assertEquals(Money.of(1.00, "USD"), result.fees().getFirst().feeAmount());
        assertEquals(Money.of(0.20, "USD"), result.agentCommission());
    }

    @Test
    @DisplayName("Uses flat fee from FeeConfig when configured in DB")
    void calculate_prefersFeeConfigFromDb() {
        when(fxRateRepository.getRate("USD", "KHR")).thenReturn(new BigDecimal("4040.00"));
        when(feeConfigRepository.findByServiceTypeIgnoreCase("P2P_TRANSFER")).thenReturn(List.of(
                new FeeConfig("P2P_TRANSFER", "SERVICE_FEE", new BigDecimal("1.25"), null, "USD")
        ));

        CalculationContext context = context("USD", "KHR", "USD", new BigDecimal("50.00"));

        CalculationResult result = strategy.calculate(context);

        assertEquals(Money.of(1.25, "USD"), result.fees().getFirst().feeAmount());
        assertEquals(Money.of(0.25, "USD"), result.agentCommission()); // 20% of 1.25
    }

    private static CalculationContext context(
            String sourceCurrency,
            String targetCurrency,
            String txnCurrency,
            BigDecimal txnAmount
    ) {
        return new CalculationContext(
                "P2P_TRANSFER",
                "ACC_USD_123",
                "ACC_KHR_456",
                sourceCurrency,
                targetCurrency,
                txnCurrency,
                txnAmount,
                "RETAIL"
        );
    }
}
