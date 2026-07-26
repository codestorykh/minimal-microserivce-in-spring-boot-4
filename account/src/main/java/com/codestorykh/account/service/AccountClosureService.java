package com.codestorykh.account.service;

import com.codestorykh.account.constant.ApiErrorCode;
import com.codestorykh.account.domain.Account;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.api.response.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountClosureService {

    private final AccountRepository accountRepository;

    public AccountClosureService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(
                        ApiErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountNumber,
                        HttpStatus.NOT_FOUND,
                        null
                ));

        // Validation 1: Check pending holds
        if (account.getHoldAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    ApiErrorCode.ACCOUNT_HAS_PENDING_HOLDS,
                    "Cannot close account with active hold amount: " + account.getHoldAmount(),
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        // Validation 2: Check remaining balance
        if (account.getBookBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    ApiErrorCode.ACCOUNT_HAS_PENDING_HOLDS,
                    "Account balance must be 0.00 before closing. Current balance: " + account.getBookBalance(),
                    HttpStatus.BAD_REQUEST,
                    null
            );
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }
}