package com.codestorykh.account.service.impl;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.request.AmlNewAccountCheckRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.dto.response.AmlInspectionResultDto;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.service.AccountOpeningService;
import com.codestorykh.account.service.InternalAmlInspectionService;
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
    private final KycOnboardingService kycOnboardingService;
    private final AccountOpeningService accountOpeningService;
    private final InternalAmlInspectionService amlInspectionService;

    @Transactional
    @Override
    public AccountOnboardingResponse onboardCustomerWithAccount(AccountOnboardingRequest request) {
        Customer customer = kycOnboardingService.resolveOrCreateCustomer(request);
        Account account = accountOpeningService.createAccount(customer, request);

        AmlInspectionResultDto amlResult = amlInspectionService.inspectNewAccount(
                AmlNewAccountCheckRequest.from(account, request)
        );

        AccountOnboardingResponse response = onboardingMapper.toAccountResponse(account);
        response.setAmlRiskLevel(amlResult.riskLevel().name());
        response.setAmlDecision(amlResult.decision().name());
        response.setAmlSummary(amlResult.summary());
        return response;
    }

    @Override
    public AccountOnboardingResponse fetchByAccountNumber(String accountNumber) {
        Optional<Account> account = accountRepository.findAccountByAccountNumber(accountNumber);
        return account.map(onboardingMapper::toAccountResponse).orElse(null);
    }
}
