package com.automation.dashboard;

import com.automation.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

/** Fail olan testler için yalnızca teknik bilgi içeren düz metin log üretir. */
public final class FailureLogWriter {

    private static final Path LOG_DIR = Path.of("target", "test-history", "logs");
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT);
    private static final DateTimeFormatter LOG_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String SELENIUM_VERSION = resolveSeleniumVersion();

    private FailureLogWriter() {
    }

    public static String write(DashboardTestEntry entry, ITestResult result) {
        try {
            Files.createDirectories(LOG_DIR);
            String casePart = sanitize(entry.caseId == null || entry.caseId.isBlank() ? "FAIL" : entry.caseId);
            String fileName = casePart + "_" + LocalDateTime.now().format(FILE_TS) + ".txt";
            Path logFile = LOG_DIR.resolve(fileName);
            Files.writeString(logFile, buildContent(entry, result), StandardCharsets.UTF_8);
            return "logs/" + fileName;
        } catch (IOException e) {
            System.out.println("[LOG-WARN] Hata logu yazılamadı: " + e.getMessage());
            return "";
        }
    }

    private static String buildContent(DashboardTestEntry entry, ITestResult result) {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();

        sb.append("HATA RAPORU — TEKNİK LOG\n");
        sb.append("=========================\n");
        sb.append("Tarih: ").append(now.format(LOG_TS)).append('\n');
        sb.append("Test Sınıfı: ").append(testClassName(result)).append('\n');
        sb.append("Test Metodu: ").append(testMethodName(result)).append('\n');
        sb.append("Senaryo ID: ").append(nullSafe(entry.caseId)).append('\n');
        sb.append("Modül: ").append(nullSafe(entry.module)).append("\n\n");

        appendEnvironment(sb);
        appendExecution(sb, result);
        appendErrorMessage(sb, result);
        appendFullStackTrace(sb, result);

        return sb.toString();
    }

    private static void appendEnvironment(StringBuilder sb) {
        sb.append("ENVIRONMENT\n");
        sb.append("-----------\n");
        sb.append("OS: ")
                .append(System.getProperty("os.name", "—"))
                .append(' ')
                .append(System.getProperty("os.version", ""))
                .append(" (")
                .append(System.getProperty("os.arch", "—"))
                .append(")\n");
        sb.append("Java: ").append(System.getProperty("java.version", "—")).append('\n');
        sb.append("Browser: ").append(browserLabel()).append('\n');
        sb.append("ChromeDriver: ").append(chromeDriverLabel()).append('\n');
        sb.append("Selenium: ").append(SELENIUM_VERSION).append('\n');
        sb.append("Base URL: ").append(configOrDash("base.url")).append('\n');
        sb.append("Headless: ").append(configOrDash("headless")).append('\n');
        sb.append("Implicit Wait: ").append(configOrDash("implicit.wait.seconds")).append(" sn\n");
        sb.append("Explicit Wait: ").append(configOrDash("explicit.wait.seconds")).append(" sn\n\n");
    }

    private static void appendExecution(StringBuilder sb, ITestResult result) {
        Long startMs = longAttr(result, "testExecutionStartMs");
        Long endMs = longAttr(result, "testExecutionEndMs");
        Long durationMs = longAttr(result, "testExecutionDurationMs");

        sb.append("TEST EXECUTION\n");
        sb.append("--------------\n");
        sb.append("Başlangıç: ").append(formatEpochMs(startMs)).append('\n');
        sb.append("Bitiş: ").append(formatEpochMs(endMs)).append('\n');
        sb.append("Toplam Süre: ").append(durationMs == null ? "—" : durationMs + "ms").append("\n\n");
    }

    private static void appendErrorMessage(StringBuilder sb, ITestResult result) {
        sb.append("HATA MESAJI\n");
        sb.append("-----------\n");
        Throwable th = result == null ? null : result.getThrowable();
        if (th == null || th.getMessage() == null || th.getMessage().isBlank()) {
            sb.append(th == null ? "—" : th.getClass().getName()).append('\n');
        } else {
            sb.append(th.getMessage()).append('\n');
        }
        sb.append('\n');
    }

    private static void appendFullStackTrace(StringBuilder sb, ITestResult result) {
        sb.append("FULL STACK TRACE\n");
        sb.append("----------------\n");
        Throwable th = result == null ? null : result.getThrowable();
        if (th == null) {
            sb.append("—\n");
            return;
        }
        StringWriter sw = new StringWriter();
        th.printStackTrace(new PrintWriter(sw));
        sb.append(sw);
    }

    private static String browserLabel() {
        String browser = ConfigManager.get("browser");
        if (browser == null || browser.isBlank()) {
            return "Chrome";
        }
        return browser.substring(0, 1).toUpperCase(Locale.ROOT) + browser.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String chromeDriverLabel() {
        try {
            WebDriverManager manager = WebDriverManager.chromedriver();
            String downloaded = manager.getDownloadedDriverPath();
            if (downloaded != null && !downloaded.isBlank()) {
                return versionFromDriverPath(downloaded);
            }
        } catch (Exception ignored) {
            // Fall through to system property.
        }
        String driverPath = System.getProperty("webdriver.chrome.driver");
        if (driverPath != null && !driverPath.isBlank()) {
            return versionFromDriverPath(driverPath);
        }
        return "WebDriverManager (sürüm alınamadı)";
    }

    private static String versionFromDriverPath(String path) {
        String normalized = path.replace('\\', '/');
        int win64 = normalized.indexOf("/win64/");
        if (win64 >= 0) {
            int start = win64 + "/win64/".length();
            int end = normalized.indexOf('/', start);
            if (end > start) {
                return normalized.substring(start, end);
            }
        }
        return path;
    }

    private static String resolveSeleniumVersion() {
        Package pkg = WebDriver.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
            return pkg.getImplementationVersion();
        }
        return "4.30.0";
    }

    private static String configOrDash(String key) {
        String value = ConfigManager.get(key);
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String testClassName(ITestResult result) {
        if (result == null || result.getTestClass() == null) {
            return "—";
        }
        return result.getTestClass().getRealClass().getSimpleName();
    }

    private static String testMethodName(ITestResult result) {
        if (result == null || result.getMethod() == null) {
            return "—";
        }
        return result.getMethod().getMethodName();
    }

    private static Long longAttr(ITestResult result, String key) {
        if (result == null) {
            return null;
        }
        Object value = result.getAttribute(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String formatEpochMs(Long epochMs) {
        if (epochMs == null) {
            return "—";
        }
        return LOG_TS.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()));
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
