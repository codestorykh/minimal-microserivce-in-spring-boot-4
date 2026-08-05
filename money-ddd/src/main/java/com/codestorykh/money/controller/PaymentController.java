package com.codestorykh.money.controller;

import com.codestorykh.money.domain.Account;
import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.AccountBalanceDto;
import com.codestorykh.money.dto.CalculationResult;
import com.codestorykh.money.dto.PaymentCalculationResultDto;
import com.codestorykh.money.dto.PaymentRequestDto;
import com.codestorykh.money.dto.PricingRequestDto;
import com.codestorykh.money.dto.ProcessPaymentRequestDto;
import com.codestorykh.money.dto.ProcessPaymentResponseDto;
import com.codestorykh.money.repository.AccountRepository;
import com.codestorykh.money.service.PaymentCalculationService;
import com.codestorykh.money.service.PaymentPricingOrchestrator;
import com.codestorykh.money.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentCalculationService calculationService;
    private final PaymentPricingOrchestrator pricingOrchestrator;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    public PaymentController(
            PaymentCalculationService calculationService,
            PaymentPricingOrchestrator pricingOrchestrator,
            TransactionService transactionService,
            AccountRepository accountRepository
    ) {
        this.calculationService = calculationService;
        this.pricingOrchestrator = pricingOrchestrator;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
    }

    /**
     * DMN quote path — FX + multi-basis fees from usd_khr_rules.dmn
     * Useful for REMITTANCE-style calculations.
     */
    @PostMapping("/calculate")
    public ResponseEntity<PaymentCalculationResultDto> calculatePayment(@RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(calculationService.calculatePayment(request));
    }

    /**
     * Strategy quote path — P2P_TRANSFER / BILL_PAYMENT pricing strategies.
     * Use this to test sender-basis vs receiver-basis product flows.
     */
    @PostMapping("/pricing")
    public ResponseEntity<CalculationResult> calculatePricing(@RequestBody PricingRequestDto request) {
        CalculationResult result = pricingOrchestrator.calculatePricing(request.toContext());
        return ResponseEntity.ok(result);
    }

    /**
     * Execute debit/credit after validation (currency match + conversion sanity).
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessPaymentResponseDto> processPayment(@RequestBody ProcessPaymentRequestDto request) {
        transactionService.processPayment(
                request.senderAccountId(),
                request.receiverAccountId(),
                new Money(request.sourceAmount(), request.sourceCurrency()),
                new Money(request.targetAmount(), request.targetCurrency()),
                request.fxRate()
        );

        Account sender = accountRepository.findByAccountNumber(request.senderAccountId()).orElseThrow();
        Account receiver = accountRepository.findByAccountNumber(request.receiverAccountId()).orElseThrow();

        return ResponseEntity.ok(new ProcessPaymentResponseDto(
                "COMPLETED",
                sender.getAccountNumber(),
                sender.getBalance(),
                sender.getCurrency(),
                receiver.getAccountNumber(),
                receiver.getBalance(),
                receiver.getCurrency()
        ));
    }

    /**
     * Helper for manual testing — inspect seeded/demo account balances.
     */
    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<AccountBalanceDto> getAccount(@PathVariable String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND: " + accountNumber));

        return ResponseEntity.ok(new AccountBalanceDto(
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance()
        ));
    }
}
