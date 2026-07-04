package com.automation.models;

public class WishListScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String productNameContains;
    private String expectedAlertContains;
    private String expectedWishListPageContains;
    private Boolean guestWishlistAccess;
    private Boolean expectWishlistAddFailure;
    private Integer wishlistAddRepeatCount;

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

    public String getExpectedAlertContains() {
        return expectedAlertContains;
    }

    public String getExpectedWishListPageContains() {
        return expectedWishListPageContains;
    }

    public Boolean getGuestWishlistAccess() {
        return guestWishlistAccess;
    }

    public Boolean getExpectWishlistAddFailure() {
        return expectWishlistAddFailure;
    }

    public Integer getWishlistAddRepeatCount() {
        return wishlistAddRepeatCount;
    }
}
