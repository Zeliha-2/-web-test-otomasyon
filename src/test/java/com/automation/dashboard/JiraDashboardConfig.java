package com.automation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class JiraDashboardConfig {
    public boolean enabled;
    public String baseUrl = "";
    public String projectKey = "QA";
    public java.util.List<String> projectKeys = java.util.List.of("QA");
    public String proxyUrl = "http://127.0.0.1:5000";
    public String channel = "Web";
    public String qaOrigin = "Test Otomasyon";
    public String qaProcess = "Regression";
    public boolean hasCredentials;
}
