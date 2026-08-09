package com.codestorykh.screening.dto.request;

import com.codestorykh.screening.watchlist.WatchlistScreeningRequest;
import com.codestorykh.screening.type.Gender;
import com.codestorykh.screening.type.IdentityType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Client-facing AML onboarding screening payload.
 * Runs before account creation — no account number required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingCheckRequest {

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    @NotBlank(message = "Identity value is required")
    private String identityNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Country is required")
    private String country;

    /** Optional second nationality / residency country */
    private String secondaryCountry;

    /** Country that issued the identity document */
    private String idIssuingCountry;

    private String city;

    /** Employer / declared organization */
    private String organization;

    private String email;

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
