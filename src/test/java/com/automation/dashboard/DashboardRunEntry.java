package com.automation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DashboardRunEntry {
    public String date;
    public String duration;
    public int total;
    public int passed;
    public int failed;
    public int skipped;
    public List<DashboardTestEntry> tests = new ArrayList<>();
}
