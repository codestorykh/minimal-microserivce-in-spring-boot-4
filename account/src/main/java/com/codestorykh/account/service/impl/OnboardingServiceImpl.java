package com.codestorykh.account.service.impl;

import com.codestorykh.account.client.screening.AccountLinkedEvent;
import com.codestorykh.account.client.screening.OnboardingCheckRequest;
import com.codestorykh.account.client.screening.ScreeningClient;
import com.codestorykh.account.client.screening.ScreeningResult;
import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.service.AccountOpeningService;
import com.codestorykh.account.service.KycOnboardingService;
import com.codestorykh.account.service.OnboardingService;
import com.codestorykh.account.service.ScreeningAccountLinkPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Onboarding flow:
 * <ol>
 *   <li>HTTP → screening: pre-check + persist screening rows ({@code account_number} null)</li>
 *   <li>Create customer/account locally</li>
 *   <li>After commit: Kafka → screening updates {@code account_number} by {@code screeningId}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingMapper onboardingMapper;
    private final AccountRepository accountRepository;
    private final KycOnboardingService kycOnboardingService;
    private final AccountOpeningService accountOpeningService;
    private final ScreeningClient screeningClient;
    private final ScreeningAccountLinkPublisher screeningAccountLinkPublisher;

    @Transactional
    @Override
    public AccountOnboardingResponse onboardCustomerWithAccount(AccountOnboardingRequest request) {
        // 1) Account → Screening (HTTP): screen KYC and save rows in screening DB
        ScreeningResult screeningResult = screeningClient.checkOnboarding(OnboardingCheckRequest.from(request));
        if (screeningResult == null) {
            throw new IllegalArgumentException("SCREENING_SERVICE_ERROR: empty onboarding screening response");
        }
        if (screeningResult.isBlocked()) {
            throw new IllegalArgumentException(
                    "SCREENING_BLOCKED: " + screeningResult.summary()
                            + " (screeningId=" + screeningResult.screeningId() + ")"
            );
        }

        // 2) Create account only when screening allows
        Customer customer = kycOnboardingService.resolveOrCreateCustomer(request);
        Account account = accountOpeningService.createAccount(customer, request);

        // 3) After DB commit: Kafka message → screening updates account_number
        screeningAccountLinkPublisher.publishAfterCommit(
                new AccountLinkedEvent(screeningResult.screeningId(), account.getAccountNumber())
        );

        AccountOnboardingResponse response = onboardingMapper.toAccountResponse(account);
        response.setScreeningRiskLevel(screeningResult.riskLevel());
        response.setScreeningDecision(screeningResult.decision());
        response.setScreeningSummary(screeningResult.summary());
        response.setScreeningId(screeningResult.screeningId());
        return response;
    }

    @Override
    public AccountOnboardingResponse fetchByAccountNumber(String accountNumber) {
        Optional<Account> account = accountRepository.findAccountByAccountNumber(accountNumber);
        return account.map(onboardingMapper::toAccountResponse).orElse(null);
    }
}
