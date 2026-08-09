package com.codestorykh.account.client.screening;

import com.codestorykh.account.dto.request.AccountOnboardingRequest;

import java.time.LocalDate;

/** JSON payload for screening service POST /api/v1/screening/onboarding/check */
public record OnboardingCheckRequest(
        String identityType,
        String identityNumber,
        String firstName,
        String lastName,
        String gender,
        LocalDate dateOfBirth,
        String country,
        String city,
        String email
) {
    public static OnboardingCheckRequest from(AccountOnboardingRequest request) {
        var address = request.getAddress();
        return new OnboardingCheckRequest(
                request.getIdentityType() != null ? request.getIdentityType().name() : null,
                request.getIdentityNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getGender() != null ? request.getGender().name() : null,
                request.getDateOfBirth(),
                address != null ? address.getCountry() : null,
                address != null ? address.getCity() : null,
                request.getEmail()
        );
    }
}
