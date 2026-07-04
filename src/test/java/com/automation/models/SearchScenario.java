package com.automation.models;

public class SearchScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String query;
    private String secondQuery;
    private String secondExpectedBodyContains;
    private boolean expectSearchRoute;
    private boolean expectProductResults;
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

    public String getQuery() {
        return query;
    }

    public String getSecondQuery() {
        return secondQuery;
    }

    public String getSecondExpectedBodyContains() {
        return secondExpectedBodyContains;
    }

    public boolean isExpectSearchRoute() {
        return expectSearchRoute;
    }

    public boolean isExpectProductResults() {
        return expectProductResults;
    }

    public String getExpectedBodyContains() {
        return expectedBodyContains;
    }
}
