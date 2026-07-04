package com.automation.models;

public class DbScenario {
    private String caseId;
    private String description;
    private String priority;
    private String technique;
    private String testType;
    private String testLevel;
    private String standard;
    private String reference;
    private String operation;
    private Integer recordId;
    private String name;
    private String category;
    private Double price;
    private Integer quantity;
    private Integer expectedCount;
    private String expectedColumn;
    private String expectedValue;
    private Boolean expectSqlError;
    /** SQL Server TestDb tablo adı (orders, products, users, test_results, failed_tests). */
    private String tableName;

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

    public String getOperation() {
        return operation;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getExpectedCount() {
        return expectedCount;
    }

    public String getExpectedColumn() {
        return expectedColumn;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public Boolean getExpectSqlError() {
        return expectSqlError;
    }

    public String getTableName() {
        return tableName;
    }
}
