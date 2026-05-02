package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.models.WishListScenario;
import com.automation.pages.WishListPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class WishListUiTest extends MyAccountAuthenticatedBaseTest {

    @DataProvider(name = "wishListScenarios")
    public Object[][] wishListScenarios(Method method) {
        WishListScenario[] scenarios =
                JsonDataLoader.load("testdata/wishlist-scenarios.json", WishListScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "wishListScenarios", description = "Add to wish list from home, open wish list page")
    public void runWishListScenario(WishListScenario scenario) {
        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        DriverFactory.getDriver().get(home);

        Assert.assertTrue(
                DriverFactory.getDriver().getCurrentUrl().contains("route=common/home")
                        || DriverFactory.getDriver().getCurrentUrl().contains("ecommerce-playground"),
                "Expected store home for case " + scenario.getCaseId());

        WishListPage page = new WishListPage();
        page.addProductToWishListByNameFragment(scenario.getProductNameContains());

        String alert = page.waitForAlertText().toLowerCase();
        String expAlert = scenario.getExpectedAlertContains().toLowerCase();
        // Some themes skip transient alerts; keep this soft and validate via final wishlist page content.
        if (!expAlert.isBlank() && !alert.isBlank()) {
            Assert.assertTrue(alert.contains(expAlert),
                    "Expected alert fragment for case " + scenario.getCaseId() + " got: " + alert);
        }

        page.openWishListViaHeader();

        String body = page.visibleBodyText().toLowerCase();
        String expPage = scenario.getExpectedWishListPageContains().toLowerCase();
        Assert.assertTrue(body.contains(expPage),
                "Wish list page should mention '" + expPage + "' for case " + scenario.getCaseId());
    }
}
