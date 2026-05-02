package com.automation.models;

public class RegisterScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;

    private String firstName;
    private String lastName;
    private String email;
    private String telephone;
    private String password;
    private String confirmPassword;
    private Boolean subscribeNewsletter;

    private Boolean expectSuccess;
    private String expectedSuccessContains;
    private String expectedWarningContains;

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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public Boolean getSubscribeNewsletter() {
        return subscribeNewsletter;
    }

    public Boolean getExpectSuccess() {
        return expectSuccess;
    }

    public String getExpectedSuccessContains() {
        return expectedSuccessContains;
    }

    public String getExpectedWarningContains() {
        return expectedWarningContains;
    }
}

