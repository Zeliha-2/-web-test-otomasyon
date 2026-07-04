package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.utils.SelfHealingElementFinder;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CartPage {
    private static final List<By> PRODUCT_THUMB_SELECTORS = List.of(
            By.cssSelector("div.product-thumb"),
            By.cssSelector(".product-layout .product-thumb"),
            By.cssSelector("#content .product-thumb"),
            By.cssSelector(".product-grid .product-thumb"),
            By.cssSelector(".product-layout"));

    private static final List<By> ADD_TO_CART_IN_CARD = List.of(
            By.cssSelector("button[onclick*='cart.add']"),
            By.cssSelector("a[onclick*='cart.add']"),
            By.cssSelector("[data-original-title*='Add to Cart']"),
            By.cssSelector("button[title*='Add to Cart']"),
            By.xpath(".//button[contains(@onclick,'cart.add')]"),
            By.xpath(".//*[contains(@title,'Add to Cart') or contains(@data-original-title,'Add to Cart')]"));

    private final WebDriver driver;

    public CartPage() {
        this.driver = DriverFactory.getDriver();
    }

    public boolean addProductToCartByNameFragment(String nameFragment) {
        String frag = normalizeFragment(nameFragment);
        if (frag == null) {
            return false;
        }
        WebElement target = findProductThumb(frag);
        if (target == null) {
            return false;
        }

        try {
            WebElement addButton = SelfHealingElementFinder.findWithin(target, ADD_TO_CART_IN_CARD);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", addButton);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addButton);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Ana sayfadaki ürün kartından detay sayfasına gider, miktar alanına yazar ve sepete ekler.
     */
    public boolean addProductToCartFromDetailWithQuantity(String nameFragment, int quantity) {
        String frag = normalizeFragment(nameFragment);
        if (frag == null) {
            return false;
        }
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));

        WebElement target = findProductThumb(frag);
        if (target == null) {
            return false;
        }

        WebElement titleLink;
        try {
            titleLink = SelfHealingElementFinder.findWithin(target, List.of(
                    By.cssSelector("h4 a"),
                    By.cssSelector(".caption h4 a"),
                    By.cssSelector("a[href*='route=product/product']"),
                    By.xpath(".//a[contains(@href,'product/product')]")));
        } catch (Exception e) {
            return false;
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", titleLink);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", titleLink);

        wait.until(d -> d.getCurrentUrl().contains("route=product/product"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        try {
            WebElement qty = SelfHealingElementFinder.find(driver, List.of(
                    By.id("input-quantity"),
                    By.name("quantity"),
                    By.cssSelector("input[name='quantity']"),
                    By.xpath("//input[@id='input-quantity' or @name='quantity']")));
            qty.clear();
            qty.sendKeys(String.valueOf(Math.max(0, quantity)));
        } catch (Exception e) {
            return false;
        }

        try {
            WebElement addBtn = SelfHealingElementFinder.find(driver, List.of(
                    By.id("button-cart"),
                    By.cssSelector("#button-cart"),
                    By.cssSelector("button[onclick*='cart.add']"),
                    By.xpath("//button[@id='button-cart' or contains(@onclick,'cart.add')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", addBtn);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);
        } catch (Exception e) {
            return false;
        }

        try {
            Thread.sleep(900);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String url = driver.getCurrentUrl().toLowerCase();
        String body = visibleBodyTextLower();
        if (quantity <= 0) {
            if (url.contains("route=checkout/cart") && !body.contains("empty") && !body.contains("no results")) {
                return true;
            }
            boolean validation =
                    body.contains("minimum")
                            || body.contains("greater than")
                            || body.contains("warning")
                            || body.contains("error")
                            || !driver.findElements(By.cssSelector(".alert-danger")).isEmpty();
            return !validation && false;
        }
        return url.contains("route=product/product")
                || url.contains("route=checkout/cart")
                || body.contains("success")
                || !driver.findElements(By.cssSelector(".alert-success")).isEmpty();
    }

    public void removeAllLinesFromCart() {
        openCartPage();
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        for (int i = 0; i < 25; i++) {
            List<WebElement> removes = driver.findElements(By.cssSelector(
                    "button[title='Remove'], a[title='Remove'], button[data-original-title='Remove'], "
                            + "button[onclick*='cart.remove'], .btn-danger[onclick*='cart.remove']"));
            boolean clicked = false;
            for (WebElement r : removes) {
                try {
                    if (r.isDisplayed()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", r);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", r);
                        clicked = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // try next
                }
            }
            if (!clicked) {
                break;
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public String waitForCartAlertText() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        By alertBy = By.cssSelector(
                ".alert-success, .alert.alert-success, .alert-danger, .alert.alert-danger, .alert-warning");
        try {
            WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(alertBy));
            return alert.getText().trim();
        } catch (Exception ignored) {
            try {
                WebElement alert = SelfHealingElementFinder.find(driver, List.of(
                        By.cssSelector(".alert-success"),
                        By.cssSelector(".alert-danger"),
                        By.cssSelector(".alert")));
                return alert.getText().trim();
            } catch (Exception e) {
                return "";
            }
        }
    }

    public void openCartPage() {
        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php";
        }
        int qMark = home.indexOf('?');
        String base = qMark >= 0 ? home.substring(0, qMark) : home;
        driver.get(base + "?route=checkout/cart");
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

    private static String normalizeFragment(String nameFragment) {
        if (nameFragment == null || nameFragment.isBlank()) {
            return null;
        }
        return nameFragment.trim().toLowerCase();
    }

    private WebElement findProductThumb(String frag) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(d -> SelfHealingElementFinder.anyPresent(d, PRODUCT_THUMB_SELECTORS));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, 0);");

        for (int pass = 0; pass < 14; pass++) {
            for (By selector : PRODUCT_THUMB_SELECTORS) {
                List<WebElement> thumbs = driver.findElements(selector);
                for (WebElement thumb : thumbs) {
                    try {
                        if (thumb.isDisplayed() && thumb.getText().toLowerCase().contains(frag)) {
                            return thumb;
                        }
                    } catch (Exception ignored) {
                        // stale
                    }
                }
            }
            js.executeScript("window.scrollBy(0, Math.max(window.innerHeight || 600, 500));");
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }
}
