package com.codestorykh.money.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/payments/calculate - DMN remittance quote")
    void testCalculatePaymentEndpoint() throws Exception {
        String requestJson = """
            {
              "serviceType": "REMITTANCE",
              "sourceCurrency": "USD",
              "targetCurrency": "KHR",
              "sourceAmount": 100.00,
              "customerTier": "RETAIL"
            }
            """;

        mockMvc.perform(post("/api/v1/payments/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAmount.amount").value(100.00))
                .andExpect(jsonPath("$.sourceAmount.currency").value("USD"))
                .andExpect(jsonPath("$.targetAmount.amount").value(404000.0))
                .andExpect(jsonPath("$.targetAmount.currency").value("KHR"))
                .andExpect(jsonPath("$.appliedFxRate").value(4040.00))
                .andExpect(jsonPath("$.totalChargeToCustomerUSD.amount").value(101.25))
                .andExpect(jsonPath("$.fees[0].feeCode").value("SERVICE_FEE_USD"))
                .andExpect(jsonPath("$.fees[0].feeAmount.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/pricing - P2P sender-basis strategy")
    void testPricingP2PEndpoint() throws Exception {
        String requestJson = """
            {
              "serviceType": "P2P_TRANSFER",
              "senderAccountId": "ACC_USD_123",
              "receiverAccountId": "ACC_KHR_456",
              "sourceCurrency": "USD",
              "targetCurrency": "KHR",
              "txnCurrency": "USD",
              "txnAmount": 100.00,
              "customerTier": "RETAIL"
            }
            """;

        mockMvc.perform(post("/api/v1/payments/pricing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceMoney.amount").value(100.00))
                .andExpect(jsonPath("$.targetMoney.amount").value(404000.0))
                .andExpect(jsonPath("$.fees[0].feeCode").value("SERVICE_FEE"))
                .andExpect(jsonPath("$.fees[0].feeAmount.amount").value(0.50))
                .andExpect(jsonPath("$.agentCommission.amount").value(0.10));
    }

    @Test
    @DisplayName("POST /api/v1/payments/pricing - Bill payment receiver-basis strategy")
    void testPricingBillPaymentEndpoint() throws Exception {
        String requestJson = """
            {
              "serviceType": "BILL_PAYMENT",
              "senderAccountId": "ACC_USD_123",
              "receiverAccountId": "ACC_KHR_456",
              "sourceCurrency": "USD",
              "targetCurrency": "KHR",
              "txnCurrency": "USD",
              "txnAmount": 100.00,
              "customerTier": "RETAIL"
            }
            """;

        mockMvc.perform(post("/api/v1/payments/pricing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fees[0].feeCode").value("BILLER_FEE"))
                .andExpect(jsonPath("$.fees[0].currencyBasis").value("RECEIVER_CURRENCY"))
                .andExpect(jsonPath("$.fees[0].feeAmount.amount").value(0.25));
    }

    @Test
    @DisplayName("GET /api/v1/payments/accounts/{id} - seeded demo account")
    void testGetAccountEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/payments/accounts/ACC_USD_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC_USD_123"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/process - debit/credit happy path")
    void testProcessPaymentEndpoint() throws Exception {
        String requestJson = """
            {
              "senderAccountId": "ACC_USD_123",
              "receiverAccountId": "ACC_KHR_456",
              "sourceAmount": 10.00,
              "sourceCurrency": "USD",
              "targetAmount": 40400.00,
              "targetCurrency": "KHR",
              "fxRate": 4040.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.senderAccountId").value("ACC_USD_123"))
                .andExpect(jsonPath("$.receiverAccountId").value("ACC_KHR_456"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/process - currency mismatch returns 400")
    void testProcessPaymentCurrencyMismatch() throws Exception {
        String requestJson = """
            {
              "senderAccountId": "ACC_USD_123",
              "receiverAccountId": "ACC_KHR_456",
              "sourceAmount": 10.00,
              "sourceCurrency": "EUR",
              "targetAmount": 40400.00,
              "targetCurrency": "KHR",
              "fxRate": 4040.00
            }
            """;

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SENDER_CURRENCY_MISMATCH"));
    }
}
