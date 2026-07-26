package com.codestorykh.account.mapper;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.type.AccountType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OnboardingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerNumber", ignore = true) // Generated in Service layer
    @Mapping(target = "fullName", expression = "java(combineFullName(request.getFirstName(), request.getLastName()))")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "accounts", ignore = true)
    // Flattening address attributes directly to Customer entity fields
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "province", source = "address.province")
    @Mapping(target = "district", source = "address.district")
    @Mapping(target = "commune", source = "address.commune")
    @Mapping(target = "village", source = "address.village")
    @Mapping(target = "streetAddress", source = "address.streetAddress")
    @Mapping(target = "addressLine2", source = "address.addressLine2")
    @Mapping(target = "postalCode", source = "address.postalCode")
    Customer toCustomerEntity(AccountOnboardingRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "bookBalance", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "holdAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "status", constant = "ACTIVE")
    Account toAccountEntity(AccountOnboardingRequest request);

    // --- ACCOUNT ENTITY -> RESPONSE DTO ---
    @Mapping(target = "customerNumber", source = "customer.customerNumber")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "bookBalance", source = "bookBalance")
    @Mapping(target = "holdBalance", source = "holdAmount")
    // Converts AccountType Enum to String value (e.g. "SAVINGS") for response DTO
    @Mapping(target = "accountType", source = "accountType", qualifiedByName = "enumToString")
    AccountOnboardingResponse toAccountResponse(Account account);

    default String combineFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    // --- CUSTOM NAMED CONVERTER HELPERS ---

    @Named("enumToString")
    default String enumToString(AccountType accountType) {
        return accountType != null ? accountType.name() : null;
    }

    // Optional: Use if request sends raw String like "SAVINGS" or "savings"
    @Named("stringToAccountType")
    default AccountType stringToAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return null;
        }
        try {
            return AccountType.valueOf(accountType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AccountType provided: " + accountType);
        }
    }

}