package com.automation.listeners;

import com.automation.utils.ScreenshotCapture;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener, IInvokedMethodListener {
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
        ExtentTest extentTest = TEST.get();
        if (extentTest != null) {
            extentTest.pass("Test passed");
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        // no-op
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod() || result.isSuccess()) {
            return;
        }
        Object existing = result.getAttribute("dashboardScreenshotPath");
        if (existing != null && !existing.toString().isBlank()) {
            return;
        }
        String screenshotPath = ScreenshotCapture.capture(screenshotLabel(result));
        result.setAttribute("dashboardScreenshotPath", screenshotPath == null ? "" : screenshotPath);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        String summary = throwable == null ? "Unknown failure" : throwable.getMessage();

        Object shot = result.getAttribute("dashboardScreenshotPath");
        String screenshotPath = shot == null ? null : shot.toString();
        if (screenshotPath != null && screenshotPath.isBlank()) {
            screenshotPath = null;
        }

        ExtentTest extentTest = TEST.get();
        if (extentTest != null) {
            extentTest.fail(summary);
            if (screenshotPath != null) {
                extentTest.addScreenCaptureFromPath(screenshotPath);
            }
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

    private static String screenshotLabel(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        Method m = result.getMethod().getConstructorOrMethod().getMethod();
        return className + "_" + m.getName();
    }
}
