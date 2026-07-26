package com.codestorykh.account.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OnboardingUtils {

    public String generateCustomerNumber() {
        // Simple customer number generator logic
        return "100" + (long) (Math.random() * 1000000000L);
    }
    public String generateAccountNumber() {
        // Simple account number generator logic
        return "100" + (long) (Math.random() * 1000000000L);
    }
}
