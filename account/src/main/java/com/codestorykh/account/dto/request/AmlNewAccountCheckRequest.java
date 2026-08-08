package com.codestorykh.account.dto.request;

import com.codestorykh.account.aml.watchlist.WatchlistScreeningRequest;
import com.codestorykh.account.type.Gender;
import com.codestorykh.account.type.IdentityType;
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
public class AmlNewAccountCheckRequest {

    @NotNull(message = "Identity type is required")
    private IdentityType idType;

    @NotBlank(message = "Identity value is required")
    private String idValue;

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

    public static AmlNewAccountCheckRequest from(AccountOnboardingRequest onboardingRequest) {
        var address = onboardingRequest.getAddress();
        return AmlNewAccountCheckRequest.builder()
                .idType(onboardingRequest.getIdentityType())
                .idValue(onboardingRequest.getIdentityNumber())
                .firstName(onboardingRequest.getFirstName())
                .lastName(onboardingRequest.getLastName())
                .gender(onboardingRequest.getGender())
                .dateOfBirth(onboardingRequest.getDateOfBirth())
                .country(address != null ? address.getCountry() : null)
                .city(address != null ? address.getCity() : null)
                .email(onboardingRequest.getEmail())
                .build();
    }

    public WatchlistScreeningRequest toWatchlistRequest() {
        return new WatchlistScreeningRequest(
                firstName,
                lastName,
                idType,
                idValue,
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
