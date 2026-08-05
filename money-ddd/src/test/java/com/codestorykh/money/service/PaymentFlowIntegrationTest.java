package com.codestorykh.money.service;

import com.codestorykh.money.domain.Account;
import com.codestorykh.money.domain.Money;
import com.codestorykh.money.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PaymentFlowIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC_USD_123", "USD", new BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC_KHR_456", "KHR", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("HAPPY PATH: Full flow debits sender and credits receiver")
    void testFullPaymentFlow_Success() {
        Money source = new Money(new BigDecimal("100.00"), "USD");
        Money target = new Money(new BigDecimal("404000.00"), "KHR");

        assertDoesNotThrow(() ->
                transactionService.processPayment(
                        "ACC_USD_123",
                        "ACC_KHR_456",
                        source,
                        target,
                        4040.00
                )
        );

        Account sender = accountRepository.findByAccountNumber("ACC_USD_123").orElseThrow();
        Account receiver = accountRepository.findByAccountNumber("ACC_KHR_456").orElseThrow();

        assertEquals(0, new BigDecimal("900.00").compareTo(sender.getBalance()));
        assertEquals(0, new BigDecimal("404000.00").compareTo(receiver.getBalance()));
    }

    @Test
    @DisplayName("UNHAPPY PATH: Rejects transaction if DB currency doesn't match requested currency")
    void testFullPaymentFlow_FailsOnCurrencyMismatch() {
        Money source = new Money(new BigDecimal("100.00"), "EUR");
        Money target = new Money(new BigDecimal("404000.00"), "KHR");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processPayment(
                        "ACC_USD_123",
                        "ACC_KHR_456",
                        source,
                        target,
                        4040.00
                )
        );

        assertTrue(exception.getMessage().contains("SENDER_CURRENCY_MISMATCH"));
    }

    @Test
    @DisplayName("CORRUPTION GUARDRAIL: Blocks transaction if conversion math is corrupted")
    void testFullPaymentFlow_FailsOnSanityGuardrail() {
        Money source = new Money(new BigDecimal("100.00"), "USD");
        Money target = new Money(new BigDecimal("4040.00"), "KHR");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transactionService.processPayment(
                        "ACC_USD_123",
                        "ACC_KHR_456",
                        source,
                        target,
                        4040.00
                )
        );

        assertTrue(exception.getMessage().contains("CRITICAL_CALCULATION_CORRUPTION"));
    }
}
