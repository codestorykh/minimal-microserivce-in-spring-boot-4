package com.codestorykh.account.service;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.utils.OnboardingUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountOpeningService {

    private final AccountRepository accountRepository;
    private final OnboardingMapper mapper;

    public AccountOpeningService(AccountRepository accountRepository, OnboardingMapper mapper) {
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    public Account createAccount(Customer customer, AccountOnboardingRequest request) {
        Account account = mapper.toAccountEntity(request);
        account.setCustomer(customer);
        account.setAccountNumber(OnboardingUtils.generateAccountNumber());
        account.setBookBalance(BigDecimal.ZERO);
        account.setHoldAmount(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        return accountRepository.save(account);
    }
}