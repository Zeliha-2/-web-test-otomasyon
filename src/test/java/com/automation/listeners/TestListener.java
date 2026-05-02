package com.automation.listeners;

import com.automation.base.DriverFactory;
import com.automation.utils.AiFailureAnalyzer;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    static {
        // Extent Spark FreeMarker şablonları bazı locale'lerde (ör. tr_TR) yanlış dosya adı üretebiliyor.
        Locale.setDefault(Locale.ENGLISH);
    }

    private static final ExtentReports EXTENT = createReport();
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        // no-op
    }

    @Override
    public void onTestStart(ITestResult result) {
        String label = result.getTestClass().getRealClass().getSimpleName()
                + " | "
                + result.getMethod().getMethodName();
        TEST.set(EXTENT.createTest(label));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TEST.get().pass("Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String screenshotPath = takeScreenshot(
                result.getTestClass().getRealClass().getSimpleName() + "_" + result.getMethod().getMethodName());
        Throwable throwable = result.getThrowable();
        String summary = throwable == null ? "Unknown failure" : throwable.getMessage();
        String aiAnalysis = AiFailureAnalyzer.analyze(summary);

        TEST.get().fail(summary);
        TEST.get().info(aiAnalysis);
        if (screenshotPath != null) {
            TEST.get().addScreenCaptureFromPath(screenshotPath);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        try {
            Locale.setDefault(Locale.ENGLISH);
            EXTENT.flush();
        } catch (Exception e) {
            System.err.println("[EXTENT-WARN] Report flush failed (tests may still be valid): " + e.getMessage());
        }
    }

    private static ExtentReports createReport() {
        String reportPath = "target/reports/extent-report.html";
        new File("target/reports").mkdirs();
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setReportName("AI 3-Layer Automation Report");
        spark.config().setDocumentTitle("Test Results");
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        return extent;
    }

    private String takeScreenshot(String testName) {
        try {
            if (!(DriverFactory.getDriver() instanceof TakesScreenshot driver)) {
                return null;
            }
            byte[] file = driver.getScreenshotAs(OutputType.BYTES);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path path = Path.of("target", "screenshots", testName + "_" + timestamp + ".png");
            Files.createDirectories(path.getParent());
            Files.write(path, file);
            return path.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
