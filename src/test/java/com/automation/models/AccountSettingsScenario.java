package com.automation.models;

public class AccountSettingsScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    /** edit-information | change-password | address-book */
    private String flow;

    private String firstName;
    private String lastName;
    private String telephone;

    private String newPassword;
    private String confirmNewPassword;

    private String addressFirstName;
    private String addressLastName;
    private String addressLine1;
    private String city;
    private String postcode;
    private Boolean expectSuccess;
    private String expectedMessageContains;

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

    public String getFlow() {
        return flow;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public String getAddressFirstName() {
        return addressFirstName;
    }

    public String getAddressLastName() {
        return addressLastName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getPostcode() {
        return postcode;
    }

    public Boolean getExpectSuccess() {
        return expectSuccess;
    }

    public String getExpectedMessageContains() {
        return expectedMessageContains;
    }
}
