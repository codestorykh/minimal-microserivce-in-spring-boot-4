package com.codestorykh.account.controller;

import com.codestorykh.account.dto.request.AccountOnboardingRequest;
import com.codestorykh.account.dto.response.AccountOnboardingResponse;
import com.codestorykh.account.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@Tag(
        name = "Customer & Account Onboarding",
        description = "Endpoints for registering new customer CIFs and creating primary CASA accounts"
)
public class AccountOnboardingController {

    private final OnboardingService onboardingService;

    public AccountOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Onboard new customer and open CASA account",
            description = "Atomically creates a new customer CIF, records demographic/address information, and opens an active CASA account."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Customer and Account created successfully",
            content = @Content(schema = @Schema(implementation = AccountOnboardingResponse.class))
    )
    public ResponseEntity<AccountOnboardingResponse> onboardCustomerAccount(
            @Valid @RequestBody AccountOnboardingRequest request) {

        AccountOnboardingResponse response = onboardingService.onboardCustomerWithAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountOnboardingResponse> findByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.status(HttpStatus.OK).body(onboardingService.fetchByAccountNumber(accountNumber));
    }
}