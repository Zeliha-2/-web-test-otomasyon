package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AccountProfileEditPage {
    private final WebDriver driver;

    public AccountProfileEditPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void open() {
        driver.get(AccountUrlHelper.route("account/edit"));
    }

    public void updateProfile(String firstName, String lastName, String telephone) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input-firstname")));

        setIfPresent(By.id("input-firstname"), firstName);
        setIfPresent(By.id("input-lastname"), lastName);
        setIfPresent(By.id("input-telephone"), telephone);

        WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#content input.btn-primary[type='submit'], #content button[type='submit'], #content input[value='Continue']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submit);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit);
    }

    private void setIfPresent(By by, String value) {
        if (value == null) {
            return;
        }
        WebElement el = driver.findElement(by);
        el.clear();
        el.sendKeys(value);
    }

    public boolean hasSuccessIndication() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        try {
            return wait.until(d -> {
                String u = d.getCurrentUrl();
                if (u.contains("route=account/account")) {
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
