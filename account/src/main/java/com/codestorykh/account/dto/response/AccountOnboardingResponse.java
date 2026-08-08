package com.codestorykh.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object returned after successfully onboarding a customer and opening an account")
public class AccountOnboardingResponse {

    @Schema(description = "Unique Customer Identification Number (CIF)", example = "CIF1711200000")
    private String customerNumber;

    @Schema(description = "Full legal name of the customer", example = "Sovan Vannak")
    private String fullName;

    @Schema(description = "Primary email address", example = "sovan.vannak@codestorykh.com")
    private String email;

    @Schema(description = "Generated CASA account number", example = "100889922110")
    private String accountNumber;

    @Schema(description = "Type of account opened", example = "SAVINGS")
    @JsonProperty("account_type")
    private String accountType;

    @Schema(description = "ISO 4217 Currency Code", example = "USD")
    private String currency;

    @Schema(description = "Book balance", example = "100.00")
    private BigDecimal bookBalance;

    @Schema(description = "Hold balance", example = "99.00")
    private BigDecimal holdBalance;

    @Schema(description = "Current account status", example = "ACTIVE")
    private String status;

    @Schema(description = "AML risk level from onboarding inspection", example = "LOW")
    private String amlRiskLevel;

    @Schema(description = "AML decision from onboarding inspection", example = "ALLOW")
    private String amlDecision;

    @Schema(description = "AML inspection summary")
    private String amlSummary;
}