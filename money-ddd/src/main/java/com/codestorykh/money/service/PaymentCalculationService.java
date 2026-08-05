package com.codestorykh.money.service;

import com.codestorykh.money.domain.Money;
import com.codestorykh.money.dto.FeeItemDto;
import com.codestorykh.money.dto.PaymentCalculationResultDto;
import com.codestorykh.money.dto.PaymentRequestDto;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PaymentCalculationService {

    private final DmnRuleEngineService ruleEngineService;

    public PaymentCalculationService(DmnRuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    public PaymentCalculationResultDto calculatePayment(PaymentRequestDto request) {
        
        // --- PASS 1: FX Evaluation ---
        Money sourceMoney = new Money(request.sourceAmount(), request.sourceCurrency());

        Map<String, Object> fxResult = ruleEngineService.evaluateFx(
            request.sourceCurrency(),
            request.targetCurrency(),
            request.customerTier(),
            sourceMoney.getAmount().doubleValue()
        );

        if (fxResult.isEmpty()) {
            throw new IllegalArgumentException("No matching FX rule found for pair.");
        }

        Map<String, Object> fxSingleResult = fxResult;
        BigDecimal fxRate = new BigDecimal(fxSingleResult.get("applied_fx_rate").toString());
        BigDecimal dmnTargetAmount = new BigDecimal(fxSingleResult.get("target_amount_khr").toString());

        // Trust DMN output for the target amount, then verify it against the applied rate
        Money targetMoney = new Money(dmnTargetAmount, request.targetCurrency());

        // --- SANITY GUARDRAIL CHECK ---
        validateConversionSanity(sourceMoney, targetMoney, fxRate);

        // --- PASS 2: Multi-Basis Fee Evaluation ---
        List<Map<String, Object>> feeResults = ruleEngineService.evaluateFees(
            request.serviceType(),
            sourceMoney.getAmount().doubleValue(),
            targetMoney.getAmount().doubleValue()
        );

        List<FeeItemDto> calculatedFees = new ArrayList<>();
        Money totalFeeConvertedToUSD = new Money(BigDecimal.ZERO, "USD");

        for (Map<String, Object> ruleRow : feeResults) {
            String feeCode = (String) ruleRow.get("fee_code");
            String basis = (String) ruleRow.get("currency_basis");
            String feeCcy = (String) ruleRow.get("fee_currency");
            BigDecimal feeAmt = new BigDecimal(ruleRow.get("fee_amount").toString());

            Money feeMoney = new Money(feeAmt, feeCcy);
            calculatedFees.add(new FeeItemDto(feeCode, basis, feeMoney));

            // Convert fee to USD for single total charge calculation
            if ("USD".equalsIgnoreCase(feeCcy)) {
                totalFeeConvertedToUSD = totalFeeConvertedToUSD.add(feeMoney);
            } else if ("KHR".equalsIgnoreCase(feeCcy)) {
                // Convert KHR fee back to USD equivalent: Fee / Rate
                BigDecimal feeInUSD = feeAmt.divide(fxRate, 2, RoundingMode.HALF_UP);
                totalFeeConvertedToUSD = totalFeeConvertedToUSD.add(new Money(feeInUSD, "USD"));
            }
        }

        Money totalChargeUSD = sourceMoney.add(totalFeeConvertedToUSD);

        return new PaymentCalculationResultDto(
            sourceMoney,
            targetMoney,
            fxRate,
            calculatedFees,
            totalChargeUSD
        );
    }

    /**
     * Circuit breaker to prevent multiplying USD by 4040 and outputting 4040 USD
     */
    private void validateConversionSanity(Money source, Money target, BigDecimal appliedRate) {
        if ("USD".equalsIgnoreCase(source.getCurrency()) && "KHR".equalsIgnoreCase(target.getCurrency())) {
            
            // Recalculate back to USD to check variance
            BigDecimal reconstructedUSD = target.getAmount().divide(appliedRate, 2, RoundingMode.HALF_UP);
            BigDecimal variance = reconstructedUSD.subtract(source.getAmount()).abs();

            // If reconstructed amount differs by more than $0.05, halt execution!
            if (variance.compareTo(new BigDecimal("0.05")) > 0) {
                throw new IllegalStateException(
                    String.format("CRITICAL_SANITY_FAILURE: FX conversion deviation detected! Source: %s, Converted: %s", source, target)
                );
            }
        }
    }
}