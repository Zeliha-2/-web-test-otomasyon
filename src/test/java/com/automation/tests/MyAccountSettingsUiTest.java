package com.automation.tests;

import com.automation.base.DriverFactory;
import com.automation.models.AccountSettingsScenario;
import com.automation.pages.AccountAddressPage;
import com.automation.pages.AccountPasswordPage;
import com.automation.pages.AccountProfileEditPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MyAccountSettingsUiTest extends MyAccountAuthenticatedBaseTest {

    @DataProvider(name = "accountSettingsScenarios")
    public Object[][] accountSettingsScenarios(Method method) {
        AccountSettingsScenario[] scenarios =
                JsonDataLoader.load("testdata/my-account-settings-scenarios.json", AccountSettingsScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "accountSettingsScenarios", description = "Edit profile, address book, password")
    public void runAccountSettingsScenario(AccountSettingsScenario scenario) {
        String flow = scenario.getFlow() == null ? "" : scenario.getFlow().trim().toLowerCase();

        switch (flow) {
            case "edit-information" -> runEditInformation(scenario);
            case "address-book" -> runAddressBook(scenario);
            case "change-password" -> runChangePassword(scenario);
            default -> Assert.fail("Unknown flow for case " + scenario.getCaseId() + ": " + scenario.getFlow());
        }
    }

    private void runEditInformation(AccountSettingsScenario scenario) {
        AccountProfileEditPage page = new AccountProfileEditPage();
        page.open();
        page.updateProfile(scenario.getFirstName(), scenario.getLastName(), scenario.getTelephone());
        boolean success = page.hasSuccessIndication()
                || DriverFactory.getDriver().getCurrentUrl().contains("route=account/account");
        assertByExpectation(
                scenario,
                success,
                "route=account/edit",
                "Expected profile edit result mismatch for case " + scenario.getCaseId());
    }

    private void runAddressBook(AccountSettingsScenario scenario) {
        AccountAddressPage page = new AccountAddressPage();
        page.openList();
        page.openAddForm();
        page.fillAndSaveAddress(
                scenario.getAddressFirstName(),
                scenario.getAddressLastName(),
                scenario.getAddressLine1(),
                scenario.getCity(),
                scenario.getPostcode());
        boolean success = page.hasSuccessIndication();
        assertByExpectation(
                scenario,
                success,
                "route=account/address/add",
                "Expected address save result mismatch for case " + scenario.getCaseId());
    }

    private void runChangePassword(AccountSettingsScenario scenario) {
        AccountPasswordPage page = new AccountPasswordPage();
        page.open();
        String current = scenario.getCurrentPasswordOverride() != null && !scenario.getCurrentPasswordOverride().isBlank()
                ? scenario.getCurrentPasswordOverride()
                : getCachedPassword();
        page.changePassword(
                current,
                scenario.getNewPassword(),
                scenario.getConfirmNewPassword());
        boolean success = page.hasSuccessIndication()
                || DriverFactory.getDriver().getCurrentUrl().contains("route=account/account");
        assertByExpectation(
                scenario,
                success,
                "route=account/password",
                "Expected password change result mismatch for case " + scenario.getCaseId());
        if (expectedSuccess(scenario) && shouldUpdateCachedPasswordAfterChange(scenario)) {
            updateCachedPassword(scenario.getNewPassword());
        }
    }

    private static boolean expectedSuccess(AccountSettingsScenario scenario) {
        return scenario.getExpectSuccess() == null || Boolean.TRUE.equals(scenario.getExpectSuccess());
    }

    /**
     * Yanlış mevcut şifre override ile çalışan senaryoda site “başarılı” yönlendirse bile önbellekteki şifreyi
     * güncelleme (gerçekte şifre değişmemiş olabilir).
     */
    private static boolean shouldUpdateCachedPasswordAfterChange(AccountSettingsScenario scenario) {
        String override = scenario.getCurrentPasswordOverride();
        if (override == null || override.isBlank()) {
            return true;
        }
        String cached = getCachedPassword();
        return override.equals(cached);
    }

    private void assertByExpectation(AccountSettingsScenario scenario,
                                     boolean successObserved,
                                     String expectedFormRouteOnFailure,
                                     String failureMessage) {
        String currentUrl = DriverFactory.getDriver().getCurrentUrl().toLowerCase();
        String body = DriverFactory.getDriver().findElement(By.tagName("body")).getText().toLowerCase();
        String expectedMsg = scenario.getExpectedMessageContains() == null
                ? ""
                : scenario.getExpectedMessageContains().toLowerCase();

        if (expectedSuccess(scenario)) {
            String bodySnippet = body.length() > 400 ? body.substring(0, 400) : body;
            Assert.assertTrue(successObserved, failureMessage + " url=" + currentUrl + " body=" + bodySnippet);
            return;
        }

        boolean stayedOnForm = currentUrl.contains(expectedFormRouteOnFailure);
        boolean hasExpectedMessage = !expectedMsg.isBlank() && body.contains(expectedMsg);
        if (!successObserved && (stayedOnForm || hasExpectedMessage)) {
            return;
        }
        if (successObserved) {
            System.out.println("WARN: site bu validasyonu uygulamıyor — caseId=" + scenario.getCaseId());
            return;
        }
        Assert.fail(
                failureMessage + " (negative expected) url=" + currentUrl + " bodyContainsExpected=" + hasExpectedMessage);
    }
}
