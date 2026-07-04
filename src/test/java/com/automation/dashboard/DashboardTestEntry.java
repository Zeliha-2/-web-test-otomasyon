package com.automation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class DashboardTestEntry {
    public String name;
    public String caseId;
    public String module;
    public String status;
    public String technique = "";
    public String testType = "";
    public String testLevel = "";
    public String standard = "";
    public String reference = "";
    public String description = "";
    public String priority = "";
    public String duration = "";
    public String errorMessage = "";
    public String screenshotPath = "";
    public String errorLogPath = "";
    public String aiAnalysis = null;
    public boolean selfHealUsed;
    public String selfHealLocator = "";
    public List<String> selfHealLocators = new ArrayList<>();
    public Map<String, String> scenarioContext = new LinkedHashMap<>();
    public JiraBugReport jiraReport = null;
    public String jiraIssueKey = "";
    public String jiraIssueUrl = "";
}
