package com.codestorykh.account.service.impl;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.repository.CustomerRepository;
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
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    @Transactional
    @Override
    public AccountOnboardingResponse onboardCustomerWithAccount(AccountOnboardingRequest accountOnboardingRequest) {
        // 1. Create Customer Master Record

        Customer customer = onboardingMapper.toCustomerEntity(accountOnboardingRequest);
        customer.setCustomerNumber(generateCustomerNumber());
        Customer savedCustomer = customerRepository.save(customer);

        // 2. Open Primary CASA Account
        Account account = onboardingMapper.toAccountEntity(accountOnboardingRequest);
        account.setCustomer(savedCustomer);
        account.setAccountNumber(generateAccountNumber());

         accountRepository.save(account);

         return onboardingMapper.toAccountResponse(account);
    }

    @Override
    public AccountOnboardingResponse fetchByAccountNumber(String accountNumber) {
        Optional<Account> account = accountRepository.findAccountByAccountNumber(accountNumber);
        return account.map(onboardingMapper::toAccountResponse).orElse(null);
    }

    private String generateCustomerNumber() {
        // Simple customer number generator logic
        return "100" + (long) (Math.random() * 1000000000L);
    }
    private String generateAccountNumber() {
        // Simple account number generator logic
        return "100" + (long) (Math.random() * 1000000000L);
    }

}
