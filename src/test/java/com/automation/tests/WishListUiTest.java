package com.automation.tests;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.models.WishListScenario;
import com.automation.pages.AccountUrlHelper;
import com.automation.pages.WishListPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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
        WebDriver driver = DriverFactory.getDriver();

        if (Boolean.TRUE.equals(scenario.getGuestWishlistAccess())) {
            driver.get(AccountUrlHelper.route("account/logout"));
            driver.get(AccountUrlHelper.route("account/wishlist"));
            String u = driver.getCurrentUrl().toLowerCase();
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            Assert.assertTrue(
                    u.contains("account/login") || u.contains("route=account/login")
                            || body.contains("returning customer") || body.contains("please login"),
                    "Guest wishlist should require login for case " + scenario.getCaseId() + " url=" + u);
            return;
        }

        String home = ConfigManager.get("store.home.url");
        if (home == null || home.isBlank()) {
            home = "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        driver.get(home);

        Assert.assertTrue(
                driver.getCurrentUrl().contains("route=common/home")
                        || driver.getCurrentUrl().contains("ecommerce-playground"),
                "Expected store home for case " + scenario.getCaseId());

        WishListPage page = new WishListPage();

        if (Boolean.TRUE.equals(scenario.getExpectWishlistAddFailure())) {
            boolean ok = page.tryAddProductToWishListByNameFragment(scenario.getProductNameContains());
            Assert.assertFalse(ok, "Expected wishlist add to fail for case " + scenario.getCaseId());
            return;
        }

        int repeats = scenario.getWishlistAddRepeatCount() != null && scenario.getWishlistAddRepeatCount() > 0
                ? scenario.getWishlistAddRepeatCount()
                : 1;
        for (int i = 0; i < repeats; i++) {
            if (i > 0) {
                driver.get(home);
            }
            page.addProductToWishListByNameFragment(scenario.getProductNameContains());
        }

        String alert = page.waitForAlertText().toLowerCase();
        String expAlert = scenario.getExpectedAlertContains() == null ? "" : scenario.getExpectedAlertContains().toLowerCase();
        if (!expAlert.isBlank() && !alert.isBlank()) {
            Assert.assertTrue(
                    alert.contains(expAlert),
                    "Expected alert fragment for case " + scenario.getCaseId() + " got: " + alert);
        }

        page.openWishListViaHeader();

        String body = page.visibleBodyText().toLowerCase();
        String expPage = scenario.getExpectedWishListPageContains() == null ? "" : scenario.getExpectedWishListPageContains().toLowerCase();
        Assert.assertTrue(
                body.contains(expPage),
                "Wish list page should mention '" + expPage + "' for case " + scenario.getCaseId());
    }
}
