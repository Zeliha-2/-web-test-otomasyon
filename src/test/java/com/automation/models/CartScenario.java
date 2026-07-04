package com.automation.models;

public class CartScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String productNameContains;
    /** Örn. zero_quantity_add, empty_cart; null veya boş = mevcut ana sayfadan sepete ekleme. */
    private String cartAction;
    private Integer cartQuantity;
    private boolean expectAddSuccess;
    private String expectedCartContains;

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

    public String getCartAction() {
        return cartAction;
    }

    public Integer getCartQuantity() {
        return cartQuantity;
    }

    public boolean isExpectAddSuccess() {
        return expectAddSuccess;
    }

    public String getExpectedCartContains() {
        return expectedCartContains;
    }
}
