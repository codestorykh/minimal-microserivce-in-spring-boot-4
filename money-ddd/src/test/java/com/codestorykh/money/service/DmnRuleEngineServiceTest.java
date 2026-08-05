package com.codestorykh.money.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DmnRuleEngineServiceTest {

    private DmnRuleEngineService dmnRuleEngineService;

    @BeforeEach
    void setUp() {
        // Instantiate using a default resource loader without firing up whole Spring Context
        dmnRuleEngineService = new DmnRuleEngineService(new DefaultResourceLoader());
        dmnRuleEngineService.init();
    }

    @Test
    @DisplayName("Should apply retail rate (4040 KHR) for retail customer under $1000")
    void testFxRule_RetailCustomer() {
        Map<String, Object> result = dmnRuleEngineService.evaluateFx("USD", "KHR", "RETAIL", 100.00);

        assertFalse(result.isEmpty(), "FX result should not be empty");
        assertEquals(4040.00, Double.parseDouble(result.get("applied_fx_rate").toString()), 0.001);
        assertEquals(404000.00, Double.parseDouble(result.get("target_amount_khr").toString()), 0.001);
        assertEquals("SUCCESS", result.get("conversion_status"));
    }

    @Test
    @DisplayName("Should apply preferential VIP rate (4060 KHR) for VIP tier")
    void testFxRule_VipCustomer() {
        Map<String, Object> result = dmnRuleEngineService.evaluateFx("USD", "KHR", "VIP", 500.00);

        assertFalse(result.isEmpty());
        assertEquals(4060.00, Double.parseDouble(result.get("applied_fx_rate").toString()), 0.001);
        assertEquals(2030000.00, Double.parseDouble(result.get("target_amount_khr").toString()), 0.001);
    }

    @Test
    @DisplayName("Should evaluate both USD Source Fee and KHR Target Fees for Remittance")
    void testFeeRules_MultiBasisRemittance() {
        // $100 USD converts to 404,000 KHR
        List<Map<String, Object>> fees = dmnRuleEngineService.evaluateFees("REMITTANCE", 100.00, 404000.00);

        assertEquals(2, fees.size(), "Should trigger 2 fee rules");

        // Rule 1: USD Source Fee
        Map<String, Object> usdFeeRule = fees.stream()
                .filter(f -> "USD".equals(f.get("fee_currency")))
                .findFirst()
                .orElseThrow();
        assertEquals("SERVICE_FEE_USD", usdFeeRule.get("fee_code"));
        assertEquals("SOURCE", usdFeeRule.get("currency_basis"));
        assertEquals(1.00, Double.parseDouble(usdFeeRule.get("fee_amount").toString()), 0.001);

        // Rule 2: KHR Target Fee
        Map<String, Object> khrFeeRule = fees.stream()
                .filter(f -> "KHR".equals(f.get("fee_currency")))
                .findFirst()
                .orElseThrow();
        assertEquals("CLEARING_FEE_KHR", khrFeeRule.get("fee_code"));
        assertEquals("TARGET", khrFeeRule.get("currency_basis"));
        assertEquals(1000.00, Double.parseDouble(khrFeeRule.get("fee_amount").toString()), 0.001);
    }
}