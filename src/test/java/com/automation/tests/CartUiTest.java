package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.config.ConfigManager;
import com.automation.models.CartScenario;
import com.automation.pages.CartPage;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CartUiTest extends BaseTest {

    @Override
    protected String navigationUrl() {
        String u = ConfigManager.get("store.home.url");
        if (u == null || u.isBlank()) {
            return "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        return u;
    }

    @DataProvider(name = "cartScenarios")
    public Object[][] cartScenarios(Method method) {
        CartScenario[] scenarios = JsonDataLoader.load("testdata/cart-scenarios.json", CartScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(dataProvider = "cartScenarios", description = "Add to cart from home and verify cart page")
    public void runCartScenario(CartScenario scenario) {
        CartPage page = new CartPage();
        boolean added = page.addProductToCartByNameFragment(scenario.getProductNameContains());

        if (scenario.isExpectAddSuccess()) {
            Assert.assertTrue(added, "Expected add-to-cart success for case " + scenario.getCaseId());
            String alert = page.waitForCartAlertText().toLowerCase();
            if (!alert.isBlank()) {
                Assert.assertTrue(alert.contains("success") || alert.contains("cart"),
                        "Expected cart success-like feedback for case " + scenario.getCaseId() + " alert=" + alert);
            }
        } else {
            Assert.assertFalse(added, "Expected add-to-cart failure for case " + scenario.getCaseId());
        }

        page.openCartPage();
        String expectedInCart = scenario.getExpectedCartContains();
        if (expectedInCart != null && !expectedInCart.isBlank()) {
            Assert.assertTrue(page.cartContains(expectedInCart),
                    "Cart page should mention '" + expectedInCart + "' for case " + scenario.getCaseId());
        } else if (!scenario.isExpectAddSuccess()) {
            Assert.assertTrue(page.visibleBodyTextLower().contains("shopping cart")
                            || page.visibleBodyTextLower().contains("your shopping cart is empty"),
                    "Expected cart page empty/normal state for negative case " + scenario.getCaseId());
        }
    }
}
