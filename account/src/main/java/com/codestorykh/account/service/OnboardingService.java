package com.codestorykh.account.service;

import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;

public interface OnboardingService {

    AccountOnboardingResponse onboardCustomerWithAccount(AccountOnboardingRequest accountOnboardingRequest);

    AccountOnboardingResponse fetchByAccountNumber(String accountNumber);
}
