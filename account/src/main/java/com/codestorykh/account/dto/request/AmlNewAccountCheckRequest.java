package com.codestorykh.account.dto.request;

import com.codestorykh.account.domain.Account;
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
 * Client-facing AML onboarding check payload.
 * Account entity is never exposed — only {@link #accountNumber}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlNewAccountCheckRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

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

    public static AmlNewAccountCheckRequest from(Account account, AccountOnboardingRequest onboardingRequest) {
        return AmlNewAccountCheckRequest.builder()
                .accountNumber(account.getAccountNumber())
                .idType(onboardingRequest.getIdentityType())
                .idValue(onboardingRequest.getIdentityNumber())
                .firstName(onboardingRequest.getFirstName())
                .lastName(onboardingRequest.getLastName())
                .gender(onboardingRequest.getGender())
                .dateOfBirth(onboardingRequest.getDateOfBirth())
                .build();
    }
}
