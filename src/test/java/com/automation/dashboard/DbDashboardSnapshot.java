package com.automation.dashboard;

import java.util.ArrayList;
import java.util.List;

public class DbDashboardSnapshot {
    public String exportedAt;
    public boolean sqlAvailable;
    public int testResultsCount;
    public int failedTestsCount;
    public int ordersCount;
    public List<DbModuleStatEntry> moduleStats = new ArrayList<>();
    public List<DbRecentFailEntry> recentFailed = new ArrayList<>();
}
