package com.codestorykh.screening.type;

import lombok.Getter;

@Getter
public enum IdentityType {
    NATIONAL_ID("National Identity Card"),
    PASSPORT("Passport"),
    DRIVING_LICENSE("Driver's License"),
    FAMILY_BOOK("Family Record Book"),
    TAX_ID("Tax Identification Number (TIN)");

    private final String displayName;

    IdentityType(String displayName) {
        this.displayName = displayName;
    }
}
