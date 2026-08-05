package com.codestorykh.money.service;


import com.codestorykh.money.dto.PaymentRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCalculationServiceTest {

    @Mock
    private DmnRuleEngineService dmnRuleEngineService;

    @InjectMocks
    private PaymentCalculationService paymentCalculationService;

    @Test
    @DisplayName("CRITICAL: Guardrail must throw exception if conversion variance is corrupted")
    void testSanityGuardrail_TriggersOnBug() {
        // Corrupted DMN: $100 USD at 4040 should be 404,000 KHR, but engine returns 4,040
        Map<String, Object> corruptedFxResponse = Map.of(
                "applied_fx_rate", 4040.00,
                "target_amount_khr", 4040.00,
                "conversion_status", "SUCCESS"
        );

        when(dmnRuleEngineService.evaluateFx(anyString(), anyString(), anyString(), anyDouble()))
                .thenReturn(corruptedFxResponse);

        PaymentRequestDto request = new PaymentRequestDto(
                "REMITTANCE", "USD", "KHR", new BigDecimal("100.00"), "RETAIL"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentCalculationService.calculatePayment(request)
        );

        assertTrue(exception.getMessage().contains("CRITICAL_SANITY_FAILURE"));
    }
}
