package com.automation.pages;

import com.automation.base.DriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CheckoutPage {
    private final WebDriver driver;

    public CheckoutPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openCheckoutPage() {
        driver.get("https://ecommerce-playground.lambdatest.io/index.php?route=checkout/checkout");
    }

    public String currentUrlLower() {
        return driver.getCurrentUrl().toLowerCase();
    }

    public String visibleBodyTextLower() {
        return driver.findElement(By.tagName("body")).getText().toLowerCase();
    }
}
