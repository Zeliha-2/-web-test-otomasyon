package com.automation.base;

import com.automation.config.ConfigManager;
import java.lang.reflect.Method;
import org.openqa.selenium.WebDriverException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    @BeforeClass(alwaysRun = true)
    public void createDriverOnce() {
        DriverFactory.createDriver();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        String baseUrl = navigationUrl();
        // Transient network drops (ERR_INTERNET_DISCONNECTED) can happen on long suites.
        // Retry navigation a few times before failing the test setup.
        int maxAttempts = 3;
        WebDriverException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                DriverFactory.getDriver().get(baseUrl);
                last = null;
                break;
            } catch (WebDriverException e) {
                last = e;
                String msg = e.getMessage() == null ? "" : e.getMessage();
                boolean transientNetwork = msg.contains("ERR_INTERNET_DISCONNECTED");
                if (!transientNetwork || attempt == maxAttempts) {
                    break;
                }
                try {
                    Thread.sleep(1500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (last != null) {
            throw last;
        }
        System.out.println("[TEST-START] " + method.getName());
    }

    /** Override in subclasses that start from a page other than account login (e.g. store home). */
    protected String navigationUrl() {
        String baseUrl = ConfigManager.get("base.url");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
            System.out.println("[CONFIG-WARN] base.url missing, using default: " + baseUrl);
        }
        return baseUrl;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}
