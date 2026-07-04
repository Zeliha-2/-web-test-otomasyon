package com.automation.utils;

import com.automation.base.DriverFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class ScreenshotCapture {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotCapture() {
    }

    /**
     * Captures a PNG under {@code target/screenshots/}. Prefer calling from
     * {@code IInvokedMethodListener.afterInvocation} while the test thread still owns the driver.
     */
    public static String capture(String testName) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) {
                System.out.println("[SCREENSHOT-WARN] Driver is null for " + testName);
                return null;
            }
            if (!(driver instanceof TakesScreenshot screenshotDriver)) {
                System.out.println("[SCREENSHOT-WARN] Driver does not support screenshots for " + testName);
                return null;
            }
            focusAlertBanner(driver);
            byte[] file = screenshotDriver.getScreenshotAs(OutputType.BYTES);
            String timestamp = LocalDateTime.now().format(TIMESTAMP);
            String safeName = testName.replaceAll("[^A-Za-z0-9._-]", "_");
            Path path = Path.of("target", "screenshots", safeName + "_" + timestamp + ".png");
            Files.createDirectories(path.getParent());
            Files.write(path, file);
            return path.toString();
        } catch (Exception e) {
            System.out.println("[SCREENSHOT-WARN] Capture failed for " + testName + ": " + e.getMessage());
            return null;
        }
    }

    private static void focusAlertBanner(WebDriver driver) {
        try {
            List<By> selectors = List.of(
                    By.cssSelector(".alert-success"),
                    By.cssSelector(".alert-danger"),
                    By.cssSelector(".alert-warning"),
                    By.cssSelector(".alert.alert-success"),
                    By.cssSelector(".alert.alert-danger"));
            for (By selector : selectors) {
                List<WebElement> elements = driver.findElements(selector);
                for (WebElement element : elements) {
                    if (element.isDisplayed()) {
                        ((JavascriptExecutor) driver).executeScript(
                                "arguments[0].scrollIntoView({block:'start', behavior:'instant'});", element);
                        Thread.sleep(300);
                        return;
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Full-page screenshot is still useful if no alert is found.
        }
    }
}
