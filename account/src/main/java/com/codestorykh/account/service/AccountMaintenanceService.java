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
public class AccountMaintenanceService {

    private final AccountRepository accountRepository;

    public AccountMaintenanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account placeHold(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
            .orElseThrow(() -> new BusinessException(
                    ApiErrorCode.ACCOUNT_NOT_FOUND,
                    "Account not found: " + accountNumber,
                    HttpStatus.NOT_FOUND,
                    null
            ));

        account.setHoldAmount(account.getHoldAmount().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public void updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        Account account = accountRepository.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(
                        ApiErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountNumber,
                        HttpStatus.NOT_FOUND,
                        null
                ));

        account.setStatus(newStatus);
        accountRepository.save(account);
    }
}