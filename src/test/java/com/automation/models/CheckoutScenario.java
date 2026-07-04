package com.automation.models;

public class CheckoutScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private boolean requireProductInCart;
    private String productNameContains;
    private boolean expectCheckoutAccessible;
    private String expectedBodyContains;
    /** UI checkout sonrası SQL Server TestDb'ye sipariş yaz/oku (UiDbOrderBridge). */
    private boolean persistOrderToDb;
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

    public boolean isRequireProductInCart() {
        return requireProductInCart;
    }

    public String getProductNameContains() {
        return productNameContains;
    }

    public boolean isExpectCheckoutAccessible() {
        return expectCheckoutAccessible;
    }

    public String getExpectedBodyContains() {
        return expectedBodyContains;
    }

    public boolean isPersistOrderToDb() {
        return persistOrderToDb;
    }

    public String getExpectedDbStatus() {
        if (expectedDbStatus == null || expectedDbStatus.isBlank()) {
            return "CHECKOUT_STARTED";
        }
        return expectedDbStatus;
    }

    /** OWASP misafir checkout: CHK-SEC-01 — JSON'a ek alan koymadan caseId ile tanınır. */
    public boolean isGuestSecurityCheckoutScenario() {
        return "CHK-SEC-01".equals(caseId);
    }
}
