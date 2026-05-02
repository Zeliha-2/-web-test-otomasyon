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

public class AccountPasswordPage {
    private final WebDriver driver;

    public AccountPasswordPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void open() {
        driver.get(AccountUrlHelper.route("account/password"));
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#content input[type='password']")));

        List<WebElement> pwdInputs = driver.findElements(By.cssSelector("#content input[type='password']"));
        if (pwdInputs.size() >= 3 && currentPassword != null && !currentPassword.isBlank()) {
            fill(pwdInputs.get(0), currentPassword);
            fill(pwdInputs.get(1), newPassword);
            fill(pwdInputs.get(2), confirmPassword);
        } else {
            WebElement newP = driver.findElement(By.id("input-password"));
            WebElement conf = driver.findElement(By.id("input-confirm"));
            if (currentPassword != null && !currentPassword.isBlank()) {
                try {
                    WebElement cur = driver.findElement(By.cssSelector("input[name='old_password'], #input-old-password"));
                    fill(cur, currentPassword);
                } catch (Exception ignored) {
                    // theme has only new + confirm
                }
            }
            fill(newP, newPassword);
            fill(conf, confirmPassword);
        }

        WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#content input.btn-primary[type='submit'], #content button[type='submit'], #content input[value='Continue']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submit);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit);
    }

    private static void fill(WebElement el, String value) {
        el.clear();
        el.sendKeys(value == null ? "" : value);
    }

    public boolean hasSuccessIndication() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        try {
            return wait.until(d -> {
                if (d.getCurrentUrl().contains("route=account/account")) {
                    return true;
                }
                for (WebElement el : d.findElements(By.cssSelector(".alert-success, .alert.alert-success"))) {
                    if (el.isDisplayed() && !el.getText().isBlank()) {
                        return true;
                    }
                }
                return false;
            });
        } catch (Exception e) {
            return false;
        }
    }
}
