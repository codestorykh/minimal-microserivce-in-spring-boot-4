package com.codestorykh.account.service;

import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.CustomerRepository;
import com.codestorykh.account.utils.OnboardingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
public class KycOnboardingService {

    private final CustomerRepository customerRepository;
    private final OnboardingMapper mapper;

    public KycOnboardingService(CustomerRepository customerRepository, OnboardingMapper mapper) {
        this.customerRepository = customerRepository;
        this.mapper = mapper;
    }

    @Transactional
    public Customer resolveOrCreateCustomer(AccountOnboardingRequest request) {
        // 1. Check if Customer CIF already exists in system using Identity Type & Number
        Optional<Customer> existingCustomer = customerRepository.findCustomerByIdentityTypeAndIdentityNumber(
                request.getIdentityType(),
                request.getIdentityNumber()
        );

        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            // Same identity must also match KYC name + DOB before opening another account
            assertIdentityProfileMatches(customer, request);
            updateCustomerDetailsIfChanged(customer, request);
            return customer;
        }

        // 2. If Customer does not exist, map DTO & create a new CIF record
        Customer newCustomer = mapper.toCustomerEntity(request);
        newCustomer.setCustomerNumber(OnboardingUtils.generateCustomerNumber());
        newCustomer.setStatus("ACTIVE");

        return customerRepository.save(newCustomer);
    }

    /**
     * When identity_type + identity_number already exist, first name, last name, and DOB
     * from the request must match the stored customer; otherwise reject as data mismatch.
     */
    private void assertIdentityProfileMatches(Customer customer, AccountOnboardingRequest request) {
        boolean firstNameMatches = normalize(customer.getFirstName()).equalsIgnoreCase(normalize(request.getFirstName()));
        boolean lastNameMatches = normalize(customer.getLastName()).equalsIgnoreCase(normalize(request.getLastName()));
        boolean dobMatches = Objects.equals(customer.getDateOfBirth(), request.getDateOfBirth());

        if (!firstNameMatches || !lastNameMatches || !dobMatches) {
            throw new IllegalArgumentException(
                    "CUSTOMER_ALREADY_EXISTS: customer already existing please verify your data "
                            + "(identity_type=" + request.getIdentityType()
                            + ", identity_number=" + request.getIdentityNumber() + ")"
            );
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void updateCustomerDetailsIfChanged(Customer customer, AccountOnboardingRequest request) {
        // Keeps primary email or address updated during new account creation
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(customer.getEmail())) {
            customer.setEmail(request.getEmail());
        }
    }
}