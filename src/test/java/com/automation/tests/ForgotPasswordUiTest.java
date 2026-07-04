package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.models.ForgotPasswordScenario;
import com.automation.pages.ForgotPasswordPage;
import com.automation.utils.JsonDataLoader;
import com.automation.utils.UiEvidence;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ForgotPasswordUiTest extends BaseTest {

    @DataProvider(name = "forgotPasswordScenarios")
    public Object[][] forgotPasswordScenarios(Method method) {
        ForgotPasswordScenario[] scenarios =
                JsonDataLoader.load("testdata/forgot-password-scenarios.json", ForgotPasswordScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "forgotPasswordScenarios", description = "Data-driven forgot password scenarios")
    public void runForgotPasswordScenario(ForgotPasswordScenario scenario) {
        String loginUrl = ConfigManager.get("base.url");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }
        DriverFactory.getDriver().get(loginUrl);

        Assert.assertTrue(DriverFactory.getDriver().getCurrentUrl().contains("route=account/login"),
                "Expected login page before opening forgot password.");

        ForgotPasswordPage forgotPage = new ForgotPasswordPage();
        forgotPage.openFromLogin();

        forgotPage.requestReset(scenario.getEmail());
        String msgRaw = forgotPage.getMessage().trim();
        String msg = msgRaw.toLowerCase();
        String expected = scenario.getExpectedMessageContains().toLowerCase();

        ITestResult current = Reporter.getCurrentTestResult();
        UiEvidence.recordVisibleMessage(
                current,
                msgRaw,
                DriverFactory.getDriver().getCurrentUrl());

        if (scenario.isExpectSuccess()) {
            Assert.assertTrue(msg.contains(expected),
                    "Expected success info not found for case " + scenario.getCaseId() + " msg=" + msg);
        } else {
            Assert.assertTrue(
                    msg.contains(expected) || DriverFactory.getDriver().getCurrentUrl().contains("route=account/forgotten"),
                    "Expected failure/validation feedback not found for case " + scenario.getCaseId() + " msg=" + msg);
        }
    }
}

