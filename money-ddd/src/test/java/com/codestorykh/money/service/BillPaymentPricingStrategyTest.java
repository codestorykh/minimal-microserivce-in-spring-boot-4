package com.codestorykh.money.service;

import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.CalculationContext;
import com.codestorykh.money.dto.CalculationResult;
import com.codestorykh.money.dto.FeeBreakdown;
import com.codestorykh.money.repository.FxRateRepository;
import com.codestorykh.money.type.CurrencyBasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillPaymentPricingStrategyTest {

    @Mock
    private FxRateRepository fxRateRepository;

    @InjectMocks
    private BillPaymentPricingStrategy strategy;

    @Test
    @DisplayName("Service type is BILL_PAYMENT")
    void getServiceType_returnsBillPayment() {
        assertEquals("BILL_PAYMENT", strategy.getServiceType());
    }

    @Test
    @DisplayName("USD to KHR: converts amount and converts flat 1000 KHR biller fee to USD")
    void calculate_crossCurrency_convertsTargetAndFeeToUsd() {
        when(fxRateRepository.getRate("USD", "KHR")).thenReturn(new BigDecimal("4040.00"));

        CalculationContext context = new CalculationContext(
                "BILL_PAYMENT",
                "ACC_USD_123",
                "ACC_KHR_456",
                "USD",
                "KHR",
                "USD",
                new BigDecimal("100.00"),
                "RETAIL"
        );

        CalculationResult result = strategy.calculate(context);

        assertEquals(Money.of(100.00, "USD"), result.sourceMoney());
        assertEquals(Money.of(404000.00, "KHR"), result.targetMoney());
        assertEquals(0, new BigDecimal("4040.00").compareTo(result.appliedFxRate()));
        assertEquals("USD", result.txnCurrency());
        assertEquals(Money.of(0, "USD"), result.agentCommission());

        assertEquals(1, result.fees().size());
        FeeBreakdown fee = result.fees().getFirst();
        assertEquals("BILLER_FEE", fee.feeCode());
        assertEquals(CurrencyBasis.RECEIVER_CURRENCY, fee.currencyBasis());
        // 1000 KHR / 4040 = 0.2475 → 0.25 USD
        assertEquals(Money.of(0.25, "USD"), fee.feeAmount());

        verify(fxRateRepository).getRate("USD", "KHR");
    }

    @Test
    @DisplayName("Same currency: keeps flat 1000 fee in target currency without FX conversion")
    void calculate_sameCurrency_keepsFeeInTargetCurrency() {
        when(fxRateRepository.getRate("KHR", "KHR")).thenReturn(BigDecimal.ONE);

        CalculationContext context = new CalculationContext(
                "BILL_PAYMENT",
                "ACC_KHR_001",
                "ACC_KHR_002",
                "KHR",
                "KHR",
                "KHR",
                new BigDecimal("50000.00"),
                "RETAIL"
        );

        CalculationResult result = strategy.calculate(context);

        assertEquals(Money.of(50000.00, "KHR"), result.sourceMoney());
        assertEquals(Money.of(50000.00, "KHR"), result.targetMoney());
        assertEquals(0, BigDecimal.ONE.compareTo(result.appliedFxRate()));

        FeeBreakdown fee = result.fees().getFirst();
        assertEquals("BILLER_FEE", fee.feeCode());
        assertEquals(Money.of(1000.00, "KHR"), fee.feeAmount());
    }

    @Test
    @DisplayName("Propagates error when FX pair is unsupported")
    void calculate_unsupportedFxPair_throws() {
        when(fxRateRepository.getRate("USD", "EUR"))
                .thenThrow(new IllegalArgumentException("UNSUPPORTED_FX_PAIR: USD->EUR"));

        CalculationContext context = new CalculationContext(
                "BILL_PAYMENT",
                "ACC_USD_123",
                "ACC_EUR_999",
                "USD",
                "EUR",
                "USD",
                new BigDecimal("25.00"),
                "RETAIL"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> strategy.calculate(context)
        );

        assertTrue(exception.getMessage().contains("UNSUPPORTED_FX_PAIR"));
    }
}
