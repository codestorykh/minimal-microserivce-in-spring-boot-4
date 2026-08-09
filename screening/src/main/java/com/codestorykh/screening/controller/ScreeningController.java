package com.codestorykh.screening.controller;

import com.codestorykh.screening.dto.event.AccountLinkedEvent;
import com.codestorykh.screening.dto.request.OnboardingCheckRequest;
import com.codestorykh.screening.dto.request.TransactionCheckRequest;
import com.codestorykh.screening.dto.response.AccountLinkResponse;
import com.codestorykh.screening.dto.response.ScreeningResultDto;
import com.codestorykh.screening.service.OnboardingScreeningService;
import com.codestorykh.screening.service.TransactionScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/screening")
@Tag(name = "Screening", description = "Onboarding and transaction risk screening")
public class ScreeningController {

    private final OnboardingScreeningService onboardingScreeningService;
    private final TransactionScreeningService transactionScreeningService;

    public ScreeningController(
            OnboardingScreeningService onboardingScreeningService,
            TransactionScreeningService transactionScreeningService
    ) {
        this.onboardingScreeningService = onboardingScreeningService;
        this.transactionScreeningService = transactionScreeningService;
    }

    @PostMapping(
            path = "/onboarding/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Screen onboarding KYC before account creation",
            description = "Pre-account KYC/watchlist screen: identity, demographics, and lists."
    )
    public ResponseEntity<ScreeningResultDto> checkOnboardingAccount(
            @Valid @RequestBody OnboardingCheckRequest request
    ) {
        log.info("Checking onboarding account for request: {}", request);
        return ResponseEntity.ok(onboardingScreeningService.inspectNewAccount(request));
    }

    @PostMapping(
            path = "/transactions/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Screen a transaction before processing",
            description = "Checks account context, names, amount and currency against transaction rules."
    )
    public ResponseEntity<ScreeningResultDto> checkTransaction(
            @Valid @RequestBody TransactionCheckRequest request
    ) {
        return ResponseEntity.ok(transactionScreeningService.inspectTransaction(request));
    }

    @PostMapping(
            path = "/account-link",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Link account number to pre-onboarding screening alerts",
            description = "Updates onboarding alerts for the screeningId with the new account number. "
                    + "Returns freezeRequired=true when any linked alert is CRITICAL."
    )
    public ResponseEntity<AccountLinkResponse> linkAccount(
            @Valid @RequestBody AccountLinkedEvent event
    ) {
        boolean freezeRequired = onboardingScreeningService.linkAccountToScreening(event);
        return ResponseEntity.ok(new AccountLinkResponse(freezeRequired));
    }
}
