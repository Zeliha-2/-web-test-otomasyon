package com.automation.base;

import com.automation.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class DriverFactory {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void createDriver() {
        if (DRIVER.get() != null) {
            return;
        }

        String browser = ConfigManager.get("browser");
        boolean headless = ConfigManager.getBoolean("headless", false);
        int implicitWait = ConfigManager.getInt("implicit.wait.seconds", 5);

        if (!"chrome".equalsIgnoreCase(browser)) {
            throw new UnsupportedOperationException("Currently only chrome is supported");
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--start-maximized");
        DRIVER.set(new ChromeDriver(options));
        DRIVER.get().manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
    }

    public static void quitDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver != null) {
            webDriver.quit();
            DRIVER.remove();
        }
    }
}
