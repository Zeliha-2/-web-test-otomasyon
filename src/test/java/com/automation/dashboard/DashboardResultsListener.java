package com.automation.dashboard;

import com.automation.config.ConfigManager;
import com.automation.db.DbClient;
import com.automation.db.DbInitializer;
import com.automation.utils.AiFailureAnalyzer;
import com.automation.utils.JiraClient;
import com.automation.utils.ScreenshotCapture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.testng.IExecutionListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Builds {@code target/test-history/results.json} for the HTML dashboard. Uses
 * {@link IExecutionListener} so history is written once per Maven/Surefire execution.
 */
public class DashboardResultsListener implements ITestListener, IExecutionListener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Path HISTORY_DIR = Path.of("target", "test-history");
    private static final Path RESULTS_JSON = HISTORY_DIR.resolve("results.json");
    private static final Path DASHBOARD_HTML = HISTORY_DIR.resolve("dashboard.html");
    private static final Path JIRA_CONFIG_JSON = HISTORY_DIR.resolve("jira-config.json");
    private static final int MAX_RUNS = 50;

    private static final ThreadLocal<Long> START_MS = new ThreadLocal<>();
    private static final List<DashboardTestEntry> CURRENT_TESTS = new CopyOnWriteArrayList<>();

    private static volatile long executionStartMs;

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void onExecutionStart() {
        executionStartMs = System.currentTimeMillis();
        CURRENT_TESTS.clear();
        initSqlServerSchema();
    }

    private static void initSqlServerSchema() {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false) || !DbClient.isSqlServerAvailable()) {
            return;
        }
        try {
            DbInitializer.initialize();
        } catch (Exception ex) {
            System.out.println("[DB-WARN] TestDb init skipped: " + ex.getMessage());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        START_MS.set(System.currentTimeMillis());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finishTest(result, "passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        finishTest(result, "failed");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        finishTest(result, "skipped");
    }

    private void finishTest(ITestResult result, String status) {
        Long start = START_MS.get();
        START_MS.remove();
        long elapsed = start == null ? 0L : System.currentTimeMillis() - start;

        DashboardTestEntry e = new DashboardTestEntry();
        Class<?> realClass = result.getTestClass().getRealClass();
        String classSimple = realClass.getSimpleName();
        String module;
        if (classSimple.endsWith("UiTest")) {
            module = classSimple.substring(0, classSimple.length() - "UiTest".length());
        } else if (classSimple.endsWith("SmokeTest")) {
            module = classSimple.substring(0, classSimple.length() - "SmokeTest".length());
        } else if (classSimple.endsWith("Test")) {
            module = classSimple.substring(0, classSimple.length() - "Test".length());
        } else {
            module = classSimple;
        }
        e.module = module;

        String method = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        if (params != null) {
            for (Object p : params) {
                ScenarioMetadataExtractor.applyScenarioObject(p, e);
            }
        }
        mergeUiEvidence(result, e);
        e.name = e.caseId != null && !e.caseId.isBlank() ? method + " [" + e.caseId + "]" : method;
        e.status = status;
        e.duration = formatSeconds(elapsed);

        Throwable th = result.getThrowable();
        if (th != null) {
            String msg = th.getMessage();
            e.errorMessage = msg == null ? th.getClass().getSimpleName() : msg;
            if (e.errorMessage.length() > 2000) {
                e.errorMessage = e.errorMessage.substring(0, 2000) + "…";
            }
        }

        Object shot = result.getAttribute("dashboardScreenshotPath");
        String rawShot = shot == null ? "" : shot.toString();
        if (rawShot.isBlank() && "failed".equals(status)) {
            String label = result.getTestClass().getRealClass().getSimpleName()
                    + "_" + result.getMethod().getMethodName();
            String captured = ScreenshotCapture.capture(label);
            if (captured != null && !captured.isBlank()) {
                rawShot = captured;
                result.setAttribute("dashboardScreenshotPath", captured);
            }
        }
        if (!rawShot.isBlank()) {
            e.screenshotPath = copyScreenshotToHistory(rawShot, e.caseId);
        }

        if ("failed".equals(status)) {
            List<String> heals = SelfHealingTelemetry.drainFallbackLocators();
            if (!heals.isEmpty()) {
                e.selfHealUsed = true;
                e.selfHealLocators = new ArrayList<>(heals);
                e.selfHealLocator = heals.get(heals.size() - 1);
            }

            String summary = buildAiSummary(result, th, e);
            String aiAnalysis = AiFailureAnalyzer.analyze(
                    summary,
                    e.module,
                    e.technique,
                    e.standard,
                    e.testType,
                    e.testLevel,
                    e.selfHealUsed,
                    e.duration);
            e.aiAnalysis = aiAnalysis;
            result.setAttribute("dashboardAiAnalysis", aiAnalysis == null ? "" : aiAnalysis);
            e.jiraReport = JiraBugReportBuilder.build(e, LocalDateTime.now().format(DATE_FMT));
            long endMs = System.currentTimeMillis();
            if (start != null) {
                result.setAttribute("testExecutionStartMs", start);
                result.setAttribute("testExecutionEndMs", endMs);
                result.setAttribute("testExecutionDurationMs", elapsed);
            }
            e.errorLogPath = FailureLogWriter.write(e, result);
            if (e.jiraReport != null) {
                e.jiraReport.errorLogPath = e.errorLogPath;
                e.jiraReport.screenshotPath = e.screenshotPath;
            }
        }

        if (!"failed".equals(status)) {
            List<String> heals = SelfHealingTelemetry.drainFallbackLocators();
            if (!heals.isEmpty()) {
                e.selfHealUsed = true;
                e.selfHealLocators = new ArrayList<>(heals);
                e.selfHealLocator = heals.get(heals.size() - 1);
            }
        }

        persistToSqlServer(e);

        CURRENT_TESTS.add(e);
    }

    private static void persistToSqlServer(DashboardTestEntry e) {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false) || !DbClient.isSqlServerAvailable()) {
            return;
        }
        DbClient.insertTestResult(
                e.name,
                e.module,
                e.status,
                e.duration,
                e.errorMessage);
        if ("failed".equals(e.status)) {
            String caseRef = e.caseId != null && !e.caseId.isBlank() ? e.caseId : e.name;
            DbClient.insertFailedTest(caseRef, e.module, e.errorMessage, e.aiAnalysis);
        }
    }

    @Override
    public void onExecutionFinish() {
        long totalMs = System.currentTimeMillis() - executionStartMs;
        try {
            Files.createDirectories(HISTORY_DIR);
            copyDashboardAssetIfPresent();

            DashboardRunEntry run = new DashboardRunEntry();
            run.date = LocalDateTime.now().format(DATE_FMT);
            run.duration = formatDurationClock(totalMs);
            run.tests = new ArrayList<>(CURRENT_TESTS);
            run.total = run.tests.size();
            run.passed = (int) run.tests.stream().filter(t -> "passed".equals(t.status)).count();
            run.failed = (int) run.tests.stream().filter(t -> "failed".equals(t.status)).count();
            run.skipped = (int) run.tests.stream().filter(t -> "skipped".equals(t.status)).count();

            DashboardResultsRoot root = loadOrNew();
            root.runs.add(0, run);
            if (root.runs.size() > MAX_RUNS) {
                root.runs = new ArrayList<>(root.runs.subList(0, MAX_RUNS));
            }
            MAPPER.writeValue(RESULTS_JSON.toFile(), root);
            DbDashboardExporter.exportIfAvailable();
            exportJiraDashboardConfig();
        } catch (IOException ex) {
            System.err.println("[DASHBOARD-WARN] Could not write results.json: " + ex.getMessage());
        } finally {
            CURRENT_TESTS.clear();
        }
    }

    private static DashboardResultsRoot loadOrNew() throws IOException {
        if (!Files.isRegularFile(RESULTS_JSON)) {
            return new DashboardResultsRoot();
        }
        return MAPPER.readValue(RESULTS_JSON.toFile(), DashboardResultsRoot.class);
    }

    private static void exportJiraDashboardConfig() {
        try {
            Files.createDirectories(HISTORY_DIR);
            JiraDashboardConfig cfg = new JiraDashboardConfig();
            cfg.enabled = ConfigManager.getBoolean("jira.enabled", false);
            cfg.baseUrl = ConfigManager.get("jira.base.url") == null ? "" : ConfigManager.get("jira.base.url");
            cfg.projectKey = ConfigManager.get("jira.project.key") == null ? "QA" : ConfigManager.get("jira.project.key");
            cfg.projectKeys = parseProjectKeys(ConfigManager.get("jira.project.keys"), cfg.projectKey);
            cfg.proxyUrl = ConfigManager.get("jira.proxy.url") == null
                    ? "http://127.0.0.1:5000" : ConfigManager.get("jira.proxy.url");
            cfg.channel = ConfigManager.get("jira.channel") == null ? "Web" : ConfigManager.get("jira.channel");
            cfg.qaOrigin = ConfigManager.get("jira.qa.origin") == null
                    ? "Test Otomasyon" : ConfigManager.get("jira.qa.origin");
            cfg.qaProcess = ConfigManager.get("jira.qa.process") == null
                    ? "Regression" : ConfigManager.get("jira.qa.process");
            cfg.hasCredentials = JiraClient.isConfigured();
            MAPPER.writeValue(JIRA_CONFIG_JSON.toFile(), cfg);
        } catch (IOException ex) {
            System.err.println("[JIRA-WARN] jira-config.json yazılamadı: " + ex.getMessage());
        }
    }

    private static void copyDashboardAssetIfPresent() throws IOException {
        try (InputStream in =
                DashboardResultsListener.class.getClassLoader().getResourceAsStream("test-history/dashboard.html")) {
            if (in != null) {
                Files.copy(in, DASHBOARD_HTML, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String formatSeconds(long ms) {
        if (ms <= 0) {
            return "0.0s";
        }
        return String.format(java.util.Locale.ROOT, "%.1fs", ms / 1000.0);
    }

    /** Wall-clock style m:ss for the whole execution. */
    private static String formatDurationClock(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long m = totalSec / 60;
        long s = totalSec % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    private static void mergeUiEvidence(ITestResult result, DashboardTestEntry entry) {
        copyEvidenceAttr(result, entry, "visibleUiMessage");
        copyEvidenceAttr(result, entry, "failurePageLabel");
        copyEvidenceAttr(result, entry, "failurePageUrl");
    }

    private static void copyEvidenceAttr(ITestResult result, DashboardTestEntry entry, String key) {
        Object value = result.getAttribute(key);
        if (value != null && !value.toString().isBlank()) {
            entry.scenarioContext.put(key, value.toString().trim());
        }
    }

    private static String buildAiSummary(ITestResult result, Throwable th, DashboardTestEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Modul: ").append(e.module == null ? "—" : e.module).append('\n');
        sb.append("Case ID: ").append(e.caseId == null ? "—" : e.caseId).append('\n');
        if (e.standard != null && !e.standard.isBlank()) {
            sb.append("Standart: ").append(e.standard).append('\n');
        }
        if (e.technique != null && !e.technique.isBlank()) {
            sb.append("Teknik: ").append(e.technique).append('\n');
        }
        sb.append("Test: ").append(result.getTestClass().getRealClass().getSimpleName())
                .append('#').append(result.getMethod().getMethodName()).append('\n');
        sb.append("Hata: ").append(th == null ? "Unknown failure" : th.getMessage());
        return sb.toString();
    }

    private static String copyScreenshotToHistory(String absolutePath, String caseId) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return "";
        }
        try {
            Path src = Path.of(absolutePath);
            if (!Files.isRegularFile(src)) {
                return "";
            }
            Path destDir = HISTORY_DIR.resolve("screenshots");
            Files.createDirectories(destDir);
            String safeCase = caseId == null || caseId.isBlank() ? "FAIL" : caseId.replaceAll("[^A-Za-z0-9_-]", "_");
            String fileName = safeCase + "_" + System.currentTimeMillis() + ".png";
            Path dest = destDir.resolve(fileName);
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            return "screenshots/" + fileName;
        } catch (Exception ex) {
            System.out.println("[DASHBOARD-WARN] Screenshot kopyalanamadı: " + ex.getMessage());
            return "";
        }
    }

    private static List<String> parseProjectKeys(String raw, String fallback) {
        List<String> keys = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split("[,;\\s]+")) {
                if (!part.isBlank() && !keys.contains(part.trim())) {
                    keys.add(part.trim());
                }
            }
        }
        if (fallback != null && !fallback.isBlank() && !keys.contains(fallback)) {
            keys.add(0, fallback);
        }
        if (keys.isEmpty()) {
            keys.add("QA");
        }
        return keys;
    }

    private static String relativizeToHistory(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return "";
        }
        try {
            Path base = HISTORY_DIR.toAbsolutePath().normalize();
            Path shot = Path.of(absolutePath).toAbsolutePath().normalize();
            return base.relativize(shot).toString().replace('\\', '/');
        } catch (Exception e) {
            return absolutePath;
        }
    }
}
