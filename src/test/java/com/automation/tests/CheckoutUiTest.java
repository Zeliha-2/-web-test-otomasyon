package com.automation.tests;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.models.CheckoutScenario;
import com.automation.pages.AccountUrlHelper;
import com.automation.pages.CartPage;
import com.automation.pages.CheckoutPage;
import com.automation.utils.JsonDataLoader;
import com.automation.utils.UiDbOrderBridge;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CheckoutUiTest extends MyAccountAuthenticatedBaseTest {

    @Override
    protected String navigationUrl() {
        String u = ConfigManager.get("store.home.url");
        if (u == null || u.isBlank()) {
            return "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        return u;
    }

    @DataProvider(name = "checkoutScenarios")
    public Object[][] checkoutScenarios(Method method) {
        CheckoutScenario[] scenarios = JsonDataLoader.load("testdata/checkout-scenarios.json", CheckoutScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "checkoutScenarios", description = "Checkout MVP: access with product vs empty-cart negative")
    public void runCheckoutScenario(CheckoutScenario scenario) {
        if (scenario.isGuestSecurityCheckoutScenario()) {
            DriverFactory.getDriver().get(AccountUrlHelper.route("account/logout"));
        }
        DriverFactory.getDriver().get(navigationUrl());

        if (scenario.isRequireProductInCart()) {
            CartPage cartPage = new CartPage();
            boolean added = cartPage.addProductToCartByNameFragment(scenario.getProductNameContains());
            Assert.assertTrue(added, "Could not add product before checkout for case " + scenario.getCaseId());
            String addFeedback = cartPage.waitForCartAlertText().toLowerCase();
            if (!addFeedback.isBlank()) {
                Assert.assertTrue(addFeedback.contains("success") || addFeedback.contains("cart"),
                        "Expected add-to-cart feedback for case " + scenario.getCaseId() + " feedback=" + addFeedback);
            }
            cartPage.openCartPage();
            Assert.assertTrue(
                    cartPage.visibleBodyTextLower().contains("checkout"),
                    "Expected checkout action visibility on cart page for case " + scenario.getCaseId());
        }

        CheckoutPage checkoutPage = new CheckoutPage();
        checkoutPage.openCheckoutPage();

        String url = checkoutPage.currentUrlLower();
        String body = checkoutPage.visibleBodyTextLower();

        if (scenario.isExpectCheckoutAccessible()) {
            Assert.assertTrue(
                    url.contains("route=checkout/checkout") || url.contains("route=checkout/cart"),
                    "Expected checkout flow page for case " + scenario.getCaseId() + " url=" + url);
        } else if (scenario.isGuestSecurityCheckoutScenario()) {
            boolean onCart = url.contains("checkout/cart") || url.contains("route=checkout/cart");
            boolean loginish = url.contains("login");
            boolean accountish = url.contains("account");
            if (onCart && !loginish) {
                Assert.fail(
                        "CHK-SEC-01: Site login gerektirmeden checkout cart sayfasına erişime izin veriyor (OWASP auth bypass riski), url="
                                + url);
            }
            Assert.assertTrue(
                    loginish || accountish,
                    "CHK-SEC-01: Misafir kullanıcı login veya account sayfasına yönlendirilmeli, url=" + url);
        } else {
            Assert.assertTrue(
                    url.contains("route=checkout/cart")
                            || body.contains("your shopping cart is empty")
                            || body.contains("products marked with")
                            || url.contains("account/login")
                            || url.contains("route=account/login")
                            || body.contains("shopping cart"),
                    "Expected checkout block/redirect for case " + scenario.getCaseId() + " url=" + url);
        }

        String expected = scenario.getExpectedBodyContains();
        if (expected != null && !expected.isBlank()) {
            Assert.assertTrue(body.contains(expected.toLowerCase()),
                    "Expected body to contain '" + expected + "' for case " + scenario.getCaseId());
        }

        if (scenario.isExpectCheckoutAccessible()) {
            UiDbOrderBridge.recordAndVerifyCheckout(
                    scenario,
                    scenario.getProductNameContains(),
                    checkoutPage.currentUrlLower(),
                    getCachedEmail());
        }
    }
}
