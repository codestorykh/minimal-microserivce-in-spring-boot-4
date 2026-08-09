package com.codestorykh.screening.watchlist;

import com.codestorykh.screening.type.IdentityType;

import java.time.LocalDate;

/**
 * Normalized input for watchlist screening (onboarding or transaction).
 */
public record WatchlistScreeningRequest(
        String firstName,
        String lastName,
        IdentityType identityType,
        String identityNumber,
        LocalDate dateOfBirth,
        String country,
        String secondaryCountry,
        String idIssuingCountry,
        String city,
        String organization,
        String email,
        String accountNumber,
        String counterpartyCountry
) {
    public String fullName() {
        return WatchlistNormalizer.fullName(firstName, lastName);
    }
}
