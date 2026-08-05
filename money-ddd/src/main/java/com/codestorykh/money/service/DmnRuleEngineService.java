package com.codestorykh.money.service;

import org.camunda.bpm.dmn.engine.*;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Embedded DMN Execution Engine Service.
 * Evaluates business decision tables in memory without external database or server dependencies.
 */
@Service
public class DmnRuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(DmnRuleEngineService.class);

    private final ResourceLoader resourceLoader;
    private DmnEngine dmnEngine;
    private DmnDecision fxDecision;
    private DmnDecision feeDecision;

    // Relative path under src/main/resources
    private static final String DMN_RESOURCE_PATH = "classpath:rules/usd_khr_rules.dmn";

    public DmnRuleEngineService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Initializes the lightweight Camunda DMN Engine and compiles the DMN XML into memory
     * during application startup.
     */
    @PostConstruct
    public synchronized void init() {
        try {
            log.info("Initializing embedded Camunda DMN Engine...");
            
            // 1. Create default thread-safe DMN Engine configuration
            DmnEngineConfiguration configuration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
            this.dmnEngine = configuration.buildEngine();

            // 2. Load and compile the DMN decisions
            loadDecisions();

            log.info("DMN Engine initialized successfully. Decisions loaded: [fx_calculator, fee_calculator]");
        } catch (Exception e) {
            log.error("Failed to initialize DmnRuleEngineService", e);
            throw new IllegalStateException("Could not start payment application due to DMN initialization error", e);
        }
    }

    /**
     * Reloads DMN rules at runtime without restarting the Spring Boot application.
     */
    public synchronized void reloadRules() {
        log.info("Reloading DMN rule definitions from classpath...");
        loadDecisions();
        log.info("DMN rules reloaded successfully.");
    }

    private void loadDecisions() {
        Resource resource = resourceLoader.getResource(DMN_RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalArgumentException("DMN File not found at location: " + DMN_RESOURCE_PATH);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // Parse all decision tables defined inside the DMN XML file
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            
            for (DmnDecision decision : decisions) {
                if ("fx_calculator".equalsIgnoreCase(decision.getKey())) {
                    this.fxDecision = decision;
                } else if ("fee_calculator".equalsIgnoreCase(decision.getKey())) {
                    this.feeDecision = decision;
                }
            }

            if (this.fxDecision == null || this.feeDecision == null) {
                throw new IllegalStateException("DMN file must contain decision keys: 'fx_calculator' and 'fee_calculator'");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading DMN input stream", e);
        }
    }

    /**
     * Evaluates FX Rate Calculation Table (Hit Policy: FIRST)
     * 
     * @return Map containing: applied_fx_rate, target_amount_khr, conversion_status
     */
    public Map<String, Object> evaluateFx(String sourceCcy, String targetCcy, String customerTier, double sourceAmountUsd) {
        VariableMap variables = Variables.createVariables()
            .putValue("source_currency", sourceCcy)
            .putValue("target_currency", targetCcy)
            .putValue("customer_tier", customerTier)
            .putValue("source_amount_usd", sourceAmountUsd);

        try {
            DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(fxDecision, variables);
            
            if (result.isEmpty()) {
                log.warn("No FX rule matched for inputs: source={}, target={}, tier={}, amount={}", 
                        sourceCcy, targetCcy, customerTier, sourceAmountUsd);
                return Collections.emptyMap();
            }

            // Hit Policy FIRST returns exactly one matching row
            return result.getSingleResult().getEntryMap();
        } catch (Exception e) {
            log.error("Error evaluating FX decision table", e);
            throw new RuntimeException("FX Evaluation Engine failure", e);
        }
    }

    /**
     * Evaluates Fee & Commission Calculation Table (Hit Policy: COLLECT)
     * 
     * @return List of Maps, each map representing an applicable fee rule row.
     */
    public List<Map<String, Object>> evaluateFees(String serviceType, double sourceAmountUsd, double targetAmountKhr) {
        VariableMap variables = Variables.createVariables()
                .putValue("service_type", serviceType)
                .putValue("source_amount_usd", sourceAmountUsd)
                .putValue("target_amount_khr", targetAmountKhr);

        try {
            DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(feeDecision, variables);

            if (result.isEmpty()) {
                log.info("No fees applicable for serviceType={}, sourceAmountUsd={}, targetAmountKhr={}",
                        serviceType, sourceAmountUsd, targetAmountKhr);
                return Collections.emptyList();
            }

            // Fix: Explicitly collect entry maps from each rule result
            return result.stream()
                    .map(DmnDecisionRuleResult::getEntryMap)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error evaluating Fee decision table", e);
            throw new RuntimeException("Fee Evaluation Engine failure", e);
        }
    }

}