package com.codestorykh.account.controller;

import com.codestorykh.account.dto.request.AmlNewAccountCheckRequest;
import com.codestorykh.account.dto.request.AmlTransactionCheckRequest;
import com.codestorykh.account.dto.response.AmlInspectionResultDto;
import com.codestorykh.account.service.InternalAmlInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aml")
@Tag(name = "AML Inspection", description = "Internal AML screening for onboarding and transactions")
public class AmlInspectionController {

    private final InternalAmlInspectionService amlInspectionService;

    public AmlInspectionController(InternalAmlInspectionService amlInspectionService) {
        this.amlInspectionService = amlInspectionService;
    }

    @PostMapping(
            path = "/onboarding/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Screen onboarding KYC before account creation",
            description = "Pre-account KYC/AML screen: identity, demographics, and watchlists "
                    + "(sanctions name/NID+DOB/country, PEP, city, org, email domain, adverse media). "
                    + "Identity watchlist requires National ID + date of birth. "
                    + "No account number required. Returns screeningId for Kafka account-link after create."
    )
    public ResponseEntity<AmlInspectionResultDto> checkOnboardingAccount(
            @Valid @RequestBody AmlNewAccountCheckRequest request
    ) {
        return ResponseEntity.ok(amlInspectionService.inspectNewAccount(request));
    }

    @PostMapping(
            path = "/transactions/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Screen a transaction before processing",
            description = "Checks account number, first/last name, transaction amount and currency against AML rules."
    )
    public ResponseEntity<AmlInspectionResultDto> checkTransaction(
            @Valid @RequestBody AmlTransactionCheckRequest request
    ) {
        return ResponseEntity.ok(amlInspectionService.inspectTransaction(request));
    }
}
