package com.automation.tests;



import com.automation.base.DriverFactory;
import com.automation.models.MyAccountScenario;
import com.automation.pages.MyAccountPage;
import com.automation.utils.JsonDataLoader;

import java.lang.reflect.Method;

import org.testng.Assert;

import org.testng.annotations.DataProvider;

import org.testng.annotations.Test;



public class MyAccountUiTest extends MyAccountAuthenticatedBaseTest {



    @DataProvider(name = "myAccountScenarios")

    public Object[][] myAccountScenarios(Method method) {

        MyAccountScenario[] scenarios =

                JsonDataLoader.load("testdata/my-account-scenarios.json", MyAccountScenario[].class);

        Object[][] data = new Object[scenarios.length][1];

        for (int i = 0; i < scenarios.length; i++) {

            data[i][0] = scenarios[i];

        }

        return data;

    }



    @Test(dataProvider = "myAccountScenarios", description = "Account area after login")

    public void runMyAccountScenario(MyAccountScenario scenario) {

        MyAccountPage page = new MyAccountPage();

        String direct = scenario.getInvalidRouteUrl();
        if (direct != null && !direct.isBlank()) {
            DriverFactory.getDriver().get(direct);
        } else {
            page.openSidebarLink(scenario.getSidebarLinkText());
        }



        String url = page.currentUrl();

        String expectedUrl = scenario.getExpectedUrlContains().toLowerCase();

        Assert.assertTrue(url.contains(expectedUrl),

                "URL should contain '" + expectedUrl + "' for case " + scenario.getCaseId() + " url=" + url);



        String body = page.bodyTextLowercase();

        String expectedBody = scenario.getExpectedBodyContains().toLowerCase();

        Assert.assertTrue(body.contains(expectedBody),

                "Page should mention '" + expectedBody + "' for case " + scenario.getCaseId());

    }

}

