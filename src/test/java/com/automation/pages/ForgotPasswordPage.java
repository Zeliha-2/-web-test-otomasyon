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

public class ForgotPasswordPage {
    private final WebDriver driver;

    public ForgotPasswordPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openFromLogin() {
        SelfHealingElementFinder.find(driver, List.of(
                By.linkText("Forgotten Password"),
                By.xpath("//a[contains(@href,'route=account/forgotten')]"),
                By.xpath("//*[self::a and contains(.,'Forgotten Password')]")
        )).click();

        int seconds = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        wait.until(ExpectedConditions.urlContains("route=account/forgotten"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("form[action*='forgotten'] #input-email, #content #input-email")));
    }

    public void requestReset(String email) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(
                ConfigManager.getInt("explicit.wait.seconds", 8)));

        WebElement emailInput = SelfHealingElementFinder.find(driver, List.of(
                By.cssSelector("form[action*='forgotten'] input#input-email"),
                By.cssSelector("form[action*='forgotten'] input[name='email']"),
                By.cssSelector("#content #input-email"),
                By.id("input-email"),
                By.name("email")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", emailInput);
        wait.until(ExpectedConditions.elementToBeClickable(emailInput));
        emailInput.clear();
        emailInput.sendKeys(email == null ? "" : email);

        WebElement continueControl = SelfHealingElementFinder.find(driver, List.of(
                By.cssSelector("form[action*='forgotten'] input[type='submit']"),
                By.xpath("//form[contains(@action,'forgotten')]//button[@type='submit']"),
                By.xpath("//form[contains(@action,'forgotten')]//input[@type='submit']"),
                By.cssSelector("#content input.btn-primary[type='submit']"),
                By.xpath("//div[@id='content']//input[@type='submit' and contains(@value,'Continue')]"),
                By.xpath("//div[@id='content']//button[@type='submit' and contains(normalize-space(.),'Continue')]"),
                By.cssSelector("#content button.btn-primary[type='submit']")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", continueControl);
        wait.until(ExpectedConditions.elementToBeClickable(continueControl));
        continueControl.click();
    }

    public String getMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(
                ConfigManager.getInt("explicit.wait.seconds", 8)));
        By selector = By.cssSelector(
                ".alert-success, .alert-danger, .alert.alert-danger, .text-danger, .invalid-feedback");
        try {
            WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
            return alert.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}

