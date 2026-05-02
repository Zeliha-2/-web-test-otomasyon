package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.utils.SelfHealingElementFinder;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {
    private final WebDriver driver;

    public LoginPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void login(String email, String password) {
        WebElement emailInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-email"),
                By.name("email"),
                By.cssSelector("input[type='email']"),
                By.xpath("//input[@placeholder='E-Mail Address']"),
                By.xpath("//label[contains(.,'E-Mail')]/following::input[1]")
        ));
        emailInput.clear();
        emailInput.sendKeys(email == null ? "" : email);

        WebElement passwordInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("password"),
                By.id("input-password"),
                By.name("password"),
                By.cssSelector("input[type='password']"),
                By.xpath("//input[@type='password']"),
                By.xpath("//label[contains(.,'Password')]/following::input[1]")
        ));
        passwordInput.clear();
        passwordInput.sendKeys(password == null ? "" : password);

        SelfHealingElementFinder.find(driver, List.of(
                By.cssSelector("input[value='Login']"),
                By.cssSelector("button[type='submit']"),
                By.xpath("//input[@type='submit' and contains(@value,'Login')]"),
                By.xpath("//button[contains(.,'Login')]"),
                By.xpath("//form//input[contains(@value,'Login')]")
        )).click();
    }

    public String getWarningMessage() {
        int waitSeconds = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        By warningBy = By.cssSelector(".alert.alert-danger, .alert-danger");
        try {
            WebElement warning = wait.until(ExpectedConditions.visibilityOfElementLocated(warningBy));
            return warning.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
