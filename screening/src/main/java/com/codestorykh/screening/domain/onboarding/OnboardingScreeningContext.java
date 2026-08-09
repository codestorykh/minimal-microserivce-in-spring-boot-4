package com.codestorykh.screening.domain.onboarding;

import com.codestorykh.screening.dto.request.OnboardingCheckRequest;
import com.codestorykh.screening.type.Gender;
import com.codestorykh.screening.type.IdentityType;
import com.codestorykh.screening.watchlist.WatchlistScreeningRequest;

import java.time.LocalDate;

/**
 * Value object for account-onboarding / KYC screening.
 */
public record OnboardingScreeningContext(
        IdentityType identityType,
        String identityNumber,
        String firstName,
        String lastName,
        Gender gender,
        LocalDate dateOfBirth,
        String country,
        String secondaryCountry,
        String idIssuingCountry,
        String city,
        String organization,
        String email
) {
    public static OnboardingScreeningContext from(OnboardingCheckRequest request) {
        return new OnboardingScreeningContext(
                request.getIdentityType(),
                request.getIdentityNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getGender(),
                request.getDateOfBirth(),
                request.getCountry(),
                request.getSecondaryCountry(),
                request.getIdIssuingCountry(),
                request.getCity(),
                request.getOrganization(),
                request.getEmail()
        );
    }

    public WatchlistScreeningRequest toWatchlistRequest() {
        return new WatchlistScreeningRequest(
                firstName,
                lastName,
                identityType,
                identityNumber,
                dateOfBirth,
                country,
                secondaryCountry,
                idIssuingCountry,
                city,
                organization,
                email,
                null,
                null
        );
    }
}
