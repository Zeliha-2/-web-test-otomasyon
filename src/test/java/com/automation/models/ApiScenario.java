package com.automation.models;

public class ApiScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String method;
    private String endpoint;
    private int expectedStatus;
    private String requestBody;
    private String expectedResponseField;
    private String expectedResponseValue;
    /** OWASP: Authorization header gönderilmesin. */
    private Boolean omitAuthHeader;

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

    public String getMethod() {
        return method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getExpectedResponseField() {
        return expectedResponseField;
    }

    public String getExpectedResponseValue() {
        return expectedResponseValue;
    }

    public Boolean getOmitAuthHeader() {
        return omitAuthHeader;
    }
}
