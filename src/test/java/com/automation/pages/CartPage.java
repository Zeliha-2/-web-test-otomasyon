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

public class CartPage {
    private final WebDriver driver;

    public CartPage() {
        this.driver = DriverFactory.getDriver();
    }

    public boolean addProductToCartByNameFragment(String nameFragment) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(d -> !d.findElements(By.cssSelector("div.product-thumb")).isEmpty());

        String frag = nameFragment == null ? "" : nameFragment.toLowerCase();
        List<WebElement> thumbs = driver.findElements(By.cssSelector("div.product-thumb"));
        WebElement target = null;
        for (WebElement thumb : thumbs) {
            try {
                if (thumb.isDisplayed() && thumb.getText().toLowerCase().contains(frag)) {
                    target = thumb;
                    break;
                }
            } catch (Exception ignored) {
                // stale element can happen on dynamic lists, keep searching
            }
        }
        if (target == null) {
            return false;
        }

        WebElement addButton = null;
        List<WebElement> clickables = target.findElements(By.cssSelector("button, a, [onclick*='cart.add']"));
        for (WebElement el : clickables) {
            String onClick = safeLower(el.getAttribute("onclick"));
            String title = safeLower(el.getAttribute("title"));
            String dataTitle = safeLower(el.getAttribute("data-original-title"));
            String text = safeLower(el.getText());
            String combined = onClick + " " + title + " " + dataTitle + " " + text;
            if (combined.contains("cart.add") || combined.contains("add to cart")) {
                addButton = el;
                break;
            }
        }

        if (addButton == null) {
            return false;
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", addButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addButton);
        return true;
    }

    public String waitForCartAlertText() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        try {
            WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".alert-success, .alert.alert-success, .alert-danger, .alert.alert-danger")));
            return alert.getText().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public void openCartPage() {
        driver.get("https://ecommerce-playground.lambdatest.io/index.php?route=checkout/cart");
    }

    public boolean cartContains(String expectedText) {
        if (expectedText == null || expectedText.isBlank()) {
            return false;
        }
        return visibleBodyTextLower().contains(expectedText.toLowerCase());
    }

    public String visibleBodyTextLower() {
        return driver.findElement(By.tagName("body")).getText().toLowerCase();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
