package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.models.SearchScenario;
import com.automation.pages.SearchPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SearchUiTest extends BaseTest {

    @Override
    protected String navigationUrl() {
        String u = ConfigManager.get("store.home.url");
        if (u == null || u.isBlank()) {
            return "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        return u;
    }

    @DataProvider(name = "searchScenarios")
    public Object[][] searchScenarios(Method method) {
        SearchScenario[] scenarios = JsonDataLoader.load("testdata/search-scenarios.json", SearchScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "searchScenarios", description = "Header search from store home")
    public void runSearchScenario(SearchScenario scenario) {
        String url = DriverFactory.getDriver().getCurrentUrl();
        Assert.assertTrue(url.contains("route=common/home") || url.contains("ecommerce-playground"),
                "Expected store home for case " + scenario.getCaseId());

        SearchPage searchPage = new SearchPage();
        searchPage.searchFromHeader(scenario.getQuery());

        if (scenario.isExpectSearchRoute()) {
            Assert.assertTrue(searchPage.currentUrlIndicatesSearch(),
                    "Expected search results URL for case " + scenario.getCaseId());
        }

        if (scenario.isExpectProductResults()) {
            Assert.assertTrue(searchPage.countProductThumbs() > 0,
                    "Expected at least one product for case " + scenario.getCaseId());
        }

        String needle = scenario.getExpectedBodyContains();
        if (needle != null && !needle.isBlank()) {
            Assert.assertTrue(searchPage.pageSourceLowercase().contains(needle.toLowerCase()),
                    "Page should mention '" + needle + "' for case " + scenario.getCaseId());
        } else if (!scenario.isExpectSearchRoute() && !scenario.isExpectProductResults()) {
            String u = DriverFactory.getDriver().getCurrentUrl().toLowerCase();
            Assert.assertTrue(u.contains("ecommerce-playground"),
                    "Expected to remain on store domain for case " + scenario.getCaseId());
        }
    }
}
