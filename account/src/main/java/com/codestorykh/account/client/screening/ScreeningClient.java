package com.codestorykh.account.client.screening;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ScreeningClient {

    private final RestClient screeningRestClient;

    public ScreeningClient(RestClient screeningRestClient) {
        this.screeningRestClient = screeningRestClient;
    }

    public ScreeningResult checkOnboarding(OnboardingCheckRequest request) {
        try {
            return screeningRestClient.post()
                    .uri("/api/v1/screening/onboarding/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ScreeningResult.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(
                    "SCREENING_SERVICE_ERROR: onboarding check failed — " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    public AccountLinkResponse linkAccount(AccountLinkedEvent event) {
        try {
            return screeningRestClient.post()
                    .uri("/api/v1/screening/account-link")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .body(AccountLinkResponse.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(
                    "SCREENING_SERVICE_ERROR: account link failed — " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }
}
