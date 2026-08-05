package com.codestorykh.money.service;

import com.codestorykh.money.domain.Account;
import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.TransactionContext;
import com.codestorykh.money.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final PaymentValidationEngine validationEngine;
    private final AccountRepository accountRepository;

    public TransactionService(PaymentValidationEngine validationEngine, AccountRepository accountRepository) {
        this.validationEngine = validationEngine;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void processPayment(
            String senderAccId,
            String receiverAccId,
            Money sourceMoney,
            Money targetMoney,
            double fxRate
    ) {
        Account sender = accountRepository.findByAccountNumber(senderAccId)
                .orElseThrow(() -> new IllegalArgumentException("SENDER_NOT_FOUND: " + senderAccId));
        Account receiver = accountRepository.findByAccountNumber(receiverAccId)
                .orElseThrow(() -> new IllegalArgumentException("RECEIVER_NOT_FOUND: " + receiverAccId));

        TransactionContext context = new TransactionContext(
                senderAccId,
                receiverAccId,
                sender.getCurrency(),
                receiver.getCurrency(),
                sourceMoney,
                targetMoney,
                fxRate
        );

        validationEngine.validateTransaction(context);

        sender.debit(sourceMoney.getAmount());
        receiver.credit(targetMoney.getAmount());

        accountRepository.save(sender);
        accountRepository.save(receiver);
    }
}
