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

public class WishListPage {
    private final WebDriver driver;

    public WishListPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void addProductToWishListByNameFragment(String nameFragment) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(d -> !d.findElements(By.cssSelector("div.product-thumb")).isEmpty());

        String frag = nameFragment == null ? "" : nameFragment.toLowerCase();
        List<WebElement> thumbs = driver.findElements(By.cssSelector("div.product-thumb"));
        WebElement target = null;
        for (WebElement t : thumbs) {
            try {
                if (t.isDisplayed() && t.getText().toLowerCase().contains(frag)) {
                    target = t;
                    break;
                }
            } catch (Exception ignored) {
                // stale; skip
            }
        }
        if (target == null) {
            throw new org.openqa.selenium.NoSuchElementException("No product thumb matching: " + nameFragment);
        }

        WebElement btn = null;
        List<WebElement> clickable = target.findElements(By.cssSelector("button, a, [onclick*='wishlist']"));
        for (WebElement el : clickable) {
            String oc = el.getAttribute("onclick");
            String title = el.getAttribute("title");
            String dataOrig = el.getAttribute("data-original-title");
            String combined = (oc == null ? "" : oc) + (title == null ? "" : title) + (dataOrig == null ? "" : dataOrig);
            if (combined.toLowerCase().contains("wishlist") || combined.contains("wishlist.add")) {
                btn = el;
                break;
            }
        }
        if (btn == null) {
            throw new org.openqa.selenium.NoSuchElementException("Wish list control not found in product tile");
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
    }

    public String waitForAlertText() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        try {
            wait.until(d -> {
                for (WebElement el : d.findElements(By.cssSelector(".alert-success, .alert.alert-success, .alert-danger"))) {
                    if (el.isDisplayed() && !el.getText().isBlank()) {
                        return true;
                    }
                }
                String body = d.findElement(By.tagName("body")).getText().toLowerCase();
                return body.contains("you have added") || body.contains("success:") || body.contains("success!");
            });
            for (WebElement el : driver.findElements(By.cssSelector(".alert-success, .alert.alert-success, .alert-danger"))) {
                if (el.isDisplayed() && !el.getText().isBlank()) {
                    return el.getText().trim();
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public void openWishListViaHeader() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        List<By> wishLinks = List.of(
                By.cssSelector("#top a[href*='wishlist']"),
                By.cssSelector("header a[href*='wishlist']"),
                By.xpath("//a[contains(@href,'route=account/wishlist')]"));
        WebElement link = null;
        for (By by : wishLinks) {
            List<WebElement> found = driver.findElements(by);
            for (WebElement e : found) {
                if (e.isDisplayed()) {
                    link = e;
                    break;
                }
            }
            if (link != null) {
                break;
            }
        }
        if (link == null) {
            throw new org.openqa.selenium.NoSuchElementException("Wish list header link not found");
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
    }

    public String visibleBodyText() {
        WebElement body = driver.findElement(By.tagName("body"));
        return body.getText();
    }
}
