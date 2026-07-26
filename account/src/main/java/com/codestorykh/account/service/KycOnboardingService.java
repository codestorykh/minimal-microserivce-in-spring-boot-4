package com.codestorykh.account.service;

import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.mapper.OnboardingMapper;
import com.codestorykh.account.repository.CustomerRepository;
import com.codestorykh.account.utils.OnboardingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

            // Optional: Update customer contact or address info if necessary
            updateCustomerDetailsIfChanged(customer, request);

            return customer;
        }

        // 2. If Customer does not exist, map DTO & create a new CIF record
        Customer newCustomer = mapper.toCustomerEntity(request);
        newCustomer.setCustomerNumber(OnboardingUtils.generateCustomerNumber());
        newCustomer.setStatus("ACTIVE");

        return customerRepository.save(newCustomer);
    }

    private void updateCustomerDetailsIfChanged(Customer customer, AccountOnboardingRequest request) {
        // Keeps primary email or address updated during new account creation
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(customer.getEmail())) {
            customer.setEmail(request.getEmail());
        }
    }
}