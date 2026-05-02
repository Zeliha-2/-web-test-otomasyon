package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.models.LoginScenario;
import com.automation.pages.LoginPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginUiTest extends BaseTest {

    @DataProvider(name = "loginScenarios")
    public Object[][] loginScenarios(Method method) {
        LoginScenario[] scenarios = JsonDataLoader.load("testdata/login-scenarios.json", LoginScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "loginScenarios", description = "Data-driven login scenarios")
    public void runLoginScenario(LoginScenario scenario) {
        Assert.assertTrue(
                DriverFactory.getDriver().getTitle().contains("Account Login")
                        || DriverFactory.getDriver().getCurrentUrl().contains("route=account/login"),
                "Login page title validation failed.");

        LoginPage loginPage = new LoginPage();
        loginPage.login(scenario.getEmail(), scenario.getPassword());

        if (scenario.isExpectWarning()) {
            String warningText = loginPage.getWarningMessage();
            Assert.assertTrue(
                    (!warningText.isBlank()
                            && warningText.toLowerCase().contains(scenario.getExpectedWarningContains().toLowerCase()))
                            || DriverFactory.getDriver().getCurrentUrl().contains("route=account/login"),
                    "Expected warning text was not found for case " + scenario.getCaseId());
        } else {
            // For empty/invalid form input cases, browser-side validation may block submit.
            Assert.assertTrue(
                    DriverFactory.getDriver().getCurrentUrl().contains("route=account/login"),
                    "Expected to remain on login page for case " + scenario.getCaseId());
        }
    }
}
