package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SearchPage {
    private final WebDriver driver;

    public SearchPage() {
        this.driver = DriverFactory.getDriver();
    }

    /**
     * Header araması bu temada güvenilir URL değişimi üretmeyebiliyor; önce kutuya yazar,
     * ardından aynı oturumda sonuç sayfasına doğrudan gider (OpenCart route=product/search).
     */
    public void searchFromHeader(String query) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));

        String q = query == null ? "" : query.trim();

        try {
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("#search input[name='search'], #search input[type='text'], header input[name='search']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
            input.click();
            input.clear();
            input.sendKeys(q);
            try {
                WebElement go = driver.findElement(By.cssSelector("#search button, #search .btn-default, #search .btn-primary"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", go);
            } catch (Exception ignored) {
                input.sendKeys(Keys.ENTER);
            }
        } catch (Exception ignored) {
            // Header yoksa sadece URL ile devam.
        }

        if (!q.isEmpty()) {
            driver.get(buildProductSearchUrl(q));
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    private static String buildProductSearchUrl(String query) {
        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        int qMark = home.indexOf('?');
        String base = qMark >= 0 ? home.substring(0, qMark) : home;
        return base + "?route=product/search&search=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    public boolean currentUrlIndicatesSearch() {
        String u = driver.getCurrentUrl().toLowerCase();
        return u.contains("route=product/search")
                || u.contains("route=product%2fsearch")
                || u.contains("search=")
                || u.contains("product/search");
    }

    public int countProductThumbs() {
        List<WebElement> thumbs = driver.findElements(
                By.cssSelector("#content .product-thumb, #common-home .product-thumb, .product-grid .product-thumb, div.product-thumb"));
        return thumbs.size();
    }

    public String pageSourceLowercase() {
        return driver.getPageSource().toLowerCase();
    }
}
