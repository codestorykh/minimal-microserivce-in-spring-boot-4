package com.codestorykh.account.service.impl;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.repository.CustomerRepository;
import com.codestorykh.account.service.AccountOpeningService;
import com.codestorykh.account.service.KycOnboardingService;
import com.codestorykh.account.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingMapper onboardingMapper;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final KycOnboardingService kycOnboardingService;
    private final AccountOpeningService accountOpeningService;


    @Transactional
    @Override
    public AccountOnboardingResponse onboardCustomerWithAccount(AccountOnboardingRequest accountOnboardingRequest) {

        Customer customer = kycOnboardingService.resolveOrCreateCustomer(accountOnboardingRequest);

        Account account = accountOpeningService.createAccount(customer, accountOnboardingRequest);
        return onboardingMapper.toAccountResponse(account);
    }

    @Override
    public AccountOnboardingResponse fetchByAccountNumber(String accountNumber) {
        Optional<Account> account = accountRepository.findAccountByAccountNumber(accountNumber);
        return account.map(onboardingMapper::toAccountResponse).orElse(null);
    }

}
