package com.automation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class JiraBugReport {
    public String projectKey = "";
    public String issueType = "Bug";
    public String summary = "";
    public String priority = "Medium";
    public String severity = "Minor";
    public String description = "";
    public String testSteps = "";
    public String expectedResult = "";
    public String actualResult = "";
    public String detectedAt = "";
    public String channel = "Web";
    public String qaOrigin = "Test Otomasyon";
    public String qaProcess = "Regression";
    public String frequency = "Always";
    public String assignee = "Automatic";
    public String caseId = "";
    public String module = "";
    public String technique = "";
    public String standard = "";
    public String screenshotPath = "";
    public String errorLogPath = "";
    public String attachmentFileName = "";
}
