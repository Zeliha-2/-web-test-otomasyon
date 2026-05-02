package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MyAccountPage {
    private final WebDriver driver;

    public MyAccountPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openSidebarLink(String linkText) {
        if (linkText == null || linkText.isBlank()) {
            return;
        }
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        List<By> candidates = List.of(
                By.partialLinkText(linkText),
                By.xpath("//aside//a[contains(normalize-space(.),'" + linkText + "')]"),
                By.xpath("//div[contains(@class,'column')]//a[contains(normalize-space(.),'" + linkText + "')]"),
                By.xpath("//a[contains(@href,'account') and contains(normalize-space(.),'" + linkText + "')]"));
        WebElement link = null;
        for (By by : candidates) {
            try {
                link = wait.until(ExpectedConditions.elementToBeClickable(by));
                if (link.isDisplayed()) {
                    break;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        if (link == null) {
            throw new org.openqa.selenium.NoSuchElementException("Sidebar link not found: " + linkText);
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
    }

    public String currentUrl() {
        return driver.getCurrentUrl().toLowerCase();
    }

    public String bodyTextLowercase() {
        return driver.findElement(By.tagName("body")).getText().toLowerCase();
    }
}
