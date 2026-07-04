package com.automation.models;

/**
 * UI → API → DB zincirini tanımlayan uçtan uca senaryo modeli.
 */
public class E2EScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String productNameContains;
    private int expectedApiStatus;
    private String expectedDbStatus;

    public String getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getTechnique() {
        return technique;
    }

    public String getTestType() {
        return testType;
    }

    public String getTestLevel() {
        return testLevel;
    }

    public String getStandard() {
        return standard;
    }

    public String getReference() {
        return reference;
    }

    public String getProductNameContains() {
        return productNameContains;
    }

    public int getExpectedApiStatus() {
        return expectedApiStatus <= 0 ? 201 : expectedApiStatus;
    }

    public String getExpectedDbStatus() {
        if (expectedDbStatus == null || expectedDbStatus.isBlank()) {
            return "ORDER_SYNCED";
        }
        return expectedDbStatus;
    }
}
