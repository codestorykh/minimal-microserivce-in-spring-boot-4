package com.codestorykh.screening.service;

import com.codestorykh.screening.dto.event.AccountLinkedEvent;
import com.codestorykh.screening.dto.request.OnboardingCheckRequest;
import com.codestorykh.screening.dto.response.ScreeningResultDto;

public interface OnboardingScreeningService {

    ScreeningResultDto inspectNewAccount(OnboardingCheckRequest request);

    /**
     * Links onboarding alerts to an account number.
     *
     * @return {@code true} when the account should be frozen (any CRITICAL linked alert)
     */
    boolean linkAccountToScreening(AccountLinkedEvent event);
}
