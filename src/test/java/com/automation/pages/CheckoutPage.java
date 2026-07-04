package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.utils.SelfHealingElementFinder;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CheckoutPage {
    private final WebDriver driver;

    public CheckoutPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openCheckoutPage() {
        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php";
        }
        int qMark = home.indexOf('?');
        String base = qMark >= 0 ? home.substring(0, qMark) : home;
        driver.get(base + "?route=checkout/checkout");
    }

    public void openCheckoutCartPage() {
        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php";
        }
        int qMark = home.indexOf('?');
        String base = qMark >= 0 ? home.substring(0, qMark) : home;
        driver.get(base + "?route=checkout/cart");
    }

    public String currentUrlLower() {
        return driver.getCurrentUrl().toLowerCase();
    }

    public String visibleBodyTextLower() {
        try {
            WebElement body = SelfHealingElementFinder.find(driver, List.of(
                    By.tagName("body"),
                    By.cssSelector("#content"),
                    By.cssSelector("main")));
            return body.getText().toLowerCase();
        } catch (Exception e) {
            return driver.findElement(By.tagName("body")).getText().toLowerCase();
        }
    }
}
