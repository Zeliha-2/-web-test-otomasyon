package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginSmokeTest extends BaseTest {

    @Test(description = "Login negative smoke scenario")
    public void invalidLoginShowsWarning() {
        Assert.assertTrue(DriverFactory.getDriver().getTitle().contains("Account Login"),
                "Login page title validation failed.");

        LoginPage loginPage = new LoginPage();
        loginPage.login("invalid@example.com", "invalid_password");
        String warningText = loginPage.getWarningMessage();

        Assert.assertTrue(
                warningText.contains("No match for E-Mail Address and/or Password")
                        || warningText.toLowerCase().contains("warning"),
                "UI negative login warning not displayed.");
    }
}
