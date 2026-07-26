package com.codestorykh.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 3, message = "Country must be 2 or 3 letter ISO code (e.g., KH, USA)")
    private String country;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Commune is required")
    private String commune;

    @NotBlank(message = "Village is required")
    private String village;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    private String addressLine2;

    @NotBlank(message = "Postal code is required")
    private String postalCode;
}