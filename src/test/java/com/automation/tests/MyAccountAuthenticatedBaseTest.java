package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.pages.LoginPage;
import com.automation.pages.RegisterPage;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Shared registration/login for My Account suites. Credentials are per-thread so parallel
 * {@code @Test} classes do not overwrite each other's cached user.
 */
public abstract class MyAccountAuthenticatedBaseTest extends BaseTest {

    private static final ThreadLocal<String> CACHED_EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> CACHED_PASSWORD = new ThreadLocal<>();

    protected static String getCachedEmail() {
        return CACHED_EMAIL.get();
    }

    protected static String getCachedPassword() {
        return CACHED_PASSWORD.get();
    }

    protected static void updateCachedPassword(String newPassword) {
        CACHED_PASSWORD.set(newPassword);
    }

    @BeforeClass(alwaysRun = true)
    public void prepareLoggedInUser() {
        String configuredEmail = ConfigManager.get("test.login.email");
        String configuredPassword = ConfigManager.get("test.login.password");
        if (configuredEmail != null && !configuredEmail.isBlank()
                && configuredPassword != null && !configuredPassword.isBlank()) {
            CACHED_EMAIL.set(configuredEmail.trim());
            CACHED_PASSWORD.set(configuredPassword.trim());
            return;
        }

        if (CACHED_EMAIL.get() != null && CACHED_PASSWORD.get() != null) {
            return;
        }

        WebDriver driver = DriverFactory.getDriver();
        String loginUrl = ConfigManager.get("base.url");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }
        driver.get(loginUrl);

        RegisterPage registerPage = new RegisterPage();
        registerPage.openRegisterFromLogin();

        String ts = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String email = "myacct+" + ts + "@example.com";
        String password = "Test1234!";
        CACHED_EMAIL.set(email);
        CACHED_PASSWORD.set(password);
        registerPage.register(
                "MyAcct",
                "User",
                email,
                "+15559876543",
                password,
                password,
                false
        );

        int waitSec = Math.max(ConfigManager.getInt("explicit.wait.seconds", 8), 15);
        boolean ok = registerPage.waitForRegistrationSuccess(waitSec)
                || registerPage.isOnSuccessPage();
        if (!ok) {
            throw new SkipException("Could not complete registration for My Account tests.");
        }
        registerPage.continueFromRegistrationSuccess();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitSec));
        wait.until(d -> {
            String u = d.getCurrentUrl();
            return u.contains("route=account/account")
                    || u.contains("route=account/login")
                    || u.contains("route=account/success");
        });

        if (driver.getCurrentUrl().contains("route=account/login")) {
            new LoginPage().login(email, password);
        }
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        WebDriver driver = DriverFactory.getDriver();
        driver.get("https://ecommerce-playground.lambdatest.io/index.php?route=account/logout");
        String loginUrl = ConfigManager.get("base.url");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }
        driver.get(loginUrl);
        new LoginPage().login(getCachedEmail(), getCachedPassword());

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(
                ConfigManager.getInt("explicit.wait.seconds", 8)));
        try {
            wait.until(d -> {
                String u = d.getCurrentUrl();
                return u.contains("route=account/account")
                        || u.contains("route=account/edit")
                        || u.contains("route=account/address");
            });
        } catch (Exception e) {
            throw new SkipException("Login did not reach account area for My Account tests.");
        }
        System.out.println("[TEST-START] " + method.getName());
    }
}
