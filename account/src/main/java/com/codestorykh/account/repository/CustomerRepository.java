package com.codestorykh.account.repository;

import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.type.IdentityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findCustomerByIdentityTypeAndIdentityNumber(@NotNull(message = "Identity type is required") IdentityType identityType,
                                                                   @NotBlank(message = "Identity number is required") String identityNumber);
}
