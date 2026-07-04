package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.models.RegisterScenario;
import com.automation.pages.RegisterPage;
import com.automation.config.ConfigManager;
import com.automation.utils.JsonDataLoader;
import java.util.Arrays;
import java.util.Comparator;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RegisterUiTest extends BaseTest {

    @DataProvider(name = "registerScenarios")
    public Object[][] registerScenarios(Method method) {
        RegisterScenario[] scenarios =
                JsonDataLoader.load("testdata/register-scenarios.json", RegisterScenario[].class);
        Arrays.sort(scenarios, Comparator.comparing(s -> Boolean.TRUE.equals(s.getExpectSuccess())));
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "registerScenarios", description = "Data-driven register scenarios")
    public void runRegisterScenario(RegisterScenario scenario) {
        // Driver is reused; önceki senaryo kayıt sonrası account/success'te kalabilir.
        resetSessionToLogin();
        String loginUrl = ConfigManager.get("base.url");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }

        // Start from login page, then navigate to register.
        Assert.assertTrue(
                DriverFactory.getDriver().getCurrentUrl().contains("route=account/login")
                        || DriverFactory.getDriver().getTitle().contains("Account Login"),
                "Expected login page before navigation.");

        RegisterPage registerPage = new RegisterPage();
        registerPage.openRegisterFromLogin();

        String dynamicEmail = applyDynamicTokens(scenario.getEmail());
        String dynamicFirstName = applyDynamicTokens(scenario.getFirstName());
        String dynamicLastName = applyDynamicTokens(scenario.getLastName());
        String dynamicTelephone = applyDynamicTokens(scenario.getTelephone());

        registerPage.register(
                dynamicFirstName,
                dynamicLastName,
                dynamicEmail,
                dynamicTelephone,
                applyDynamicTokens(scenario.getPassword()),
                applyDynamicTokens(scenario.getConfirmPassword()),
                scenario.getSubscribeNewsletter() != null && scenario.getSubscribeNewsletter()
        );

        int waitSec = ConfigManager.getInt("explicit.wait.seconds", 8);

        if (Boolean.TRUE.equals(scenario.getExpectSuccess())) {
            boolean success = registerPage.waitForRegistrationSuccess(Math.max(waitSec, 12));
            String banner = registerPage.getSuccessMessage();
            String snippet = registerPage.getPageVisibleTextSnippet().toLowerCase();
            String expected = scenario.getExpectedSuccessContains() == null ? "" : scenario.getExpectedSuccessContains();
            boolean textOk = !expected.isBlank()
                    && (banner.toLowerCase().contains(expected.toLowerCase())
                    || snippet.contains(expected.toLowerCase()));
            Assert.assertTrue(
                    success || registerPage.isOnSuccessPage() || textOk,
                    "Registration success not detected for case " + scenario.getCaseId()
                            + " url=" + DriverFactory.getDriver().getCurrentUrl());
            resetSessionToLogin();
        } else {
            String expectedWarning = scenario.getExpectedWarningContains() == null ? "" : scenario.getExpectedWarningContains();
            String errors = registerPage.getCombinedErrorText();
            if (!expectedWarning.isBlank()) {
                boolean serverOrInlineError = !errors.isBlank()
                        && errors.toLowerCase().contains(expectedWarning.toLowerCase());
                boolean nativeEmailValidation = false;
                if (expectedWarning.toLowerCase().contains("e-mail")) {
                    String nativeMsg = registerPage.getInputValidationMessage("email");
                    nativeEmailValidation = !nativeMsg.isBlank();
                }
                Assert.assertTrue(
                        serverOrInlineError || nativeEmailValidation,
                        "Expected validation text not found for case " + scenario.getCaseId()
                                + " errors=[" + errors + "] url=" + DriverFactory.getDriver().getCurrentUrl());
            } else {
                Assert.assertTrue(
                        DriverFactory.getDriver().getCurrentUrl().contains("route=account/register"),
                        "Expected to remain on register page for case " + scenario.getCaseId());
            }
        }
    }

    private String applyDynamicTokens(String value) {
        if (value == null) {
            return null;
        }
        // Keep generated values unique even in very fast runs.
        String ts = String.valueOf(Instant.now().toEpochMilli()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        return value.replace("${timestamp}", ts);
    }

    private void resetSessionToLogin() {
        // Force a clean state for the next scenario.
        DriverFactory.getDriver().get("https://ecommerce-playground.lambdatest.io/index.php?route=account/logout");
        String loginUrl = ConfigManager.get("base.url");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }
        DriverFactory.getDriver().get(loginUrl);
    }
}

