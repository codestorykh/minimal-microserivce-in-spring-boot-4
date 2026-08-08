package com.codestorykh.account.dto.request;

import com.codestorykh.account.dto.AddressRequest;
import com.codestorykh.account.type.AccountType;
import com.codestorykh.account.type.Gender;
import com.codestorykh.account.type.IdentityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOnboardingRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address format")
    private String email;

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    @NotBlank(message = "Identity number is required")
    private String identityNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency is required")
    private String currency; // e.g., USD, KHR

    @NotNull(message = "Address details are required")
    @Valid
    private AddressRequest address;
}