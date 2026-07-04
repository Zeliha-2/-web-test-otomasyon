package com.automation.models;

public class MyAccountScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String sidebarLinkText;
    /** Tam URL; doluysa sidebar yerine doğrudan bu adrese gidilir (geçersiz route güvenlik senaryoları). */
    private String invalidRouteUrl;
    private String expectedUrlContains;
    private String expectedBodyContains;

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

    public String getSidebarLinkText() {
        return sidebarLinkText;
    }

    public String getInvalidRouteUrl() {
        return invalidRouteUrl;
    }

    public String getExpectedUrlContains() {
        return expectedUrlContains;
    }

    public String getExpectedBodyContains() {
        return expectedBodyContains;
    }

    /** boundary-value + boş sidebar metni (MAC-BV-01) ile gezinme yapılmadan URL doğrulanır. */
    public boolean isEmptySidebarBoundaryScenario() {
        return "boundary-value".equalsIgnoreCase(technique)
                && (sidebarLinkText == null || sidebarLinkText.isBlank());
    }
}
