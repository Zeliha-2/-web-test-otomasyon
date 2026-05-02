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

public class RegisterPage {
    private final WebDriver driver;

    public RegisterPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openRegisterFromLogin() {
        SelfHealingElementFinder.find(driver, List.of(
                By.linkText("Register"),
                By.xpath("//a[contains(@href,'route=account/register')]"),
                By.xpath("//*[self::a and contains(.,'Register')]")
        )).click();
    }

    public void register(String firstName,
                           String lastName,
                           String email,
                           String telephone,
                           String password,
                           String confirmPassword,
                           boolean subscribeNewsletter) {
        WebElement firstNameInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-firstname"),
                By.name("firstname"),
                By.cssSelector("input[name='firstname']"),
                By.xpath("//input[@placeholder='First Name']"),
                By.xpath("//label[contains(.,'First Name')]/following::input[1]")
        ));
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName == null ? "" : firstName);

        WebElement lastNameInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-lastname"),
                By.name("lastname"),
                By.cssSelector("input[name='lastname']"),
                By.xpath("//input[@placeholder='Last Name']"),
                By.xpath("//label[contains(.,'Last Name')]/following::input[1]")
        ));
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName == null ? "" : lastName);

        WebElement emailInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-email"),
                By.name("email"),
                By.cssSelector("input[type='email']"),
                By.xpath("//input[@placeholder='E-Mail Address']"),
                By.xpath("//label[contains(.,'E-Mail')]/following::input[1]")
        ));
        emailInput.clear();
        emailInput.sendKeys(email == null ? "" : email);

        WebElement telephoneInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-telephone"),
                By.name("telephone"),
                By.cssSelector("input[name='telephone']"),
                By.xpath("//label[contains(.,'Telephone')]/following::input[1]"),
                By.xpath("//input[@placeholder='Telephone']")
        ));
        telephoneInput.clear();
        telephoneInput.sendKeys(telephone == null ? "" : telephone);

        WebElement passwordInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-password"),
                By.name("password"),
                By.cssSelector("input[type='password']"),
                By.xpath("//label[contains(.,'Password')]/following::input[1]"),
                By.xpath("//input[@placeholder='Password']")
        ));
        passwordInput.clear();
        passwordInput.sendKeys(password == null ? "" : password);

        WebElement confirmPasswordInput = SelfHealingElementFinder.find(driver, List.of(
                By.id("input-confirm"),
                By.name("confirm"),
                By.cssSelector("input[name='confirm']"),
                By.xpath("//input[@placeholder='Confirm Password']"),
                By.xpath("//*[contains(.,'Confirm')]/following::input[1]")
        ));
        confirmPasswordInput.clear();
        confirmPasswordInput.sendKeys(confirmPassword == null ? "" : confirmPassword);

        // Newsletter subscription (OpenCart usually uses radio buttons)
        clickNewsletterRadio(subscribeNewsletter);

        // OpenCart: registration requires Privacy Policy agreement
        agreeToPrivacyPolicy();

        SelfHealingElementFinder.find(driver, List.of(
                By.cssSelector("input[value='Continue']"),
                By.cssSelector("button[type='submit']"),
                By.xpath("//input[@type='submit' and contains(@value,'Continue')]"),
                By.xpath("//button[contains(.,'Continue')]")
        )).click();
    }

    private void agreeToPrivacyPolicy() {
        List<By> locators = List.of(
                By.name("agree"),
                By.id("input-agree"),
                By.cssSelector("input[name='agree'][type='checkbox']"),
                By.cssSelector("label[for='input-agree']")
        );
        for (By by : locators) {
            try {
                WebElement agree = driver.findElement(by);
                if ("input".equalsIgnoreCase(agree.getTagName())) {
                    if (!agree.isSelected()) {
                        try {
                            agree.click();
                        } catch (Exception clickError) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", agree);
                        }
                    }
                    ((JavascriptExecutor) driver).executeScript("arguments[0].checked=true;", agree);
                } else {
                    agree.click();
                }
                return;
            } catch (Exception ignored) {
                // try next
            }
        }
    }

    private void clickNewsletterRadio(boolean subscribe) {
        String yesValue = subscribe ? "1" : "0";
        List<By> locators = List.of(
                By.cssSelector("input[name='newsletter'][value='" + yesValue + "']"),
                By.id(subscribe ? "input-newsletter-yes" : "input-newsletter-no"),
                By.cssSelector("input[type='radio'][name='newsletter'][value='" + yesValue + "']")
        );
        for (By by : locators) {
            try {
                WebElement el = driver.findElement(by);
                if (!el.isSelected()) {
                    el.click();
                }
                return;
            } catch (Exception ignored) {
                // Try next locator
            }
        }
    }

    /**
     * Top-of-form alert + inline field errors (OpenCart often uses .text-danger per field).
     */
    public String getCombinedErrorText() {
        int waitSeconds = Math.min(ConfigManager.getInt("explicit.wait.seconds", 8), 6);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        try {
            wait.until(d -> !collectErrorTexts().isBlank());
        } catch (Exception ignored) {
            // continue with whatever we have
        }
        return collectErrorTexts();
    }

    private String collectErrorTexts() {
        StringBuilder sb = new StringBuilder();
        List<WebElement> nodes = driver.findElements(By.cssSelector(
                ".alert-danger, .alert.alert-danger, .text-danger, .invalid-feedback"));
        for (WebElement el : nodes) {
            try {
                if (el.isDisplayed()) {
                    String t = el.getText().trim();
                    if (!t.isBlank()) {
                        if (sb.length() > 0) {
                            sb.append(" | ");
                        }
                        sb.append(t);
                    }
                }
            } catch (Exception ignored) {
                // skip stale
            }
        }
        return sb.toString().trim();
    }

    public boolean isOnSuccessPage() {
        String url = driver.getCurrentUrl();
        return url.contains("route=account/success") || url.contains("account/success");
    }

    /**
     * Waits for redirect to success URL or visible success banner.
     */
    public boolean waitForRegistrationSuccess(int maxSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(maxSeconds));
        try {
            return wait.until(d -> {
                if (d.getCurrentUrl().contains("route=account/success")
                        || d.getCurrentUrl().contains("account/success")) {
                    return true;
                }
                try {
                    List<WebElement> ok = d.findElements(By.cssSelector(".alert-success"));
                    for (WebElement el : ok) {
                        if (el.isDisplayed()) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                    // ignore
                }
                return false;
            });
        } catch (Exception e) {
            return false;
        }
    }

    public String getSuccessMessage() {
        int waitSeconds = Math.min(ConfigManager.getInt("explicit.wait.seconds", 8), 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        By successBy = By.cssSelector(".alert-success, .alert.alert-success");
        try {
            WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(successBy));
            return success.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getPageVisibleTextSnippet() {
        try {
            String body = driver.findElement(By.tagName("body")).getText();
            return body == null ? "" : body.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /** Success page: continue into account area (OpenCart). */
    public void continueFromRegistrationSuccess() {
        int waitSeconds = Math.max(ConfigManager.getInt("explicit.wait.seconds", 8), 10);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        try {
            WebElement cont = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("#content a.btn-primary, a.btn-primary[href*='account']")));
            cont.click();
        } catch (Exception ignored) {
            // Already navigated away or theme differs.
        }
    }

    public String getInputValidationMessage(String name) {
        try {
            WebElement input = driver.findElement(By.name(name));
            Object msg = ((JavascriptExecutor) driver).executeScript("return arguments[0].validationMessage;", input);
            return msg == null ? "" : msg.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}

