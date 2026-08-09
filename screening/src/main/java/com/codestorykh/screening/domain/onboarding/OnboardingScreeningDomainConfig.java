package com.codestorykh.screening.domain.onboarding;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnboardingScreeningDomainConfig {

    @Bean
    OnboardingScreeningDomainService onboardingScreeningDomainService() {
        return new OnboardingScreeningDomainService();
    }
}
