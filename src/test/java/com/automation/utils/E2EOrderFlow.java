package com.automation.utils;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import com.automation.db.DbClient;
import com.automation.models.E2EScenario;
import com.automation.pages.CartPage;
import com.automation.pages.CheckoutPage;
import com.automation.pages.SearchPage;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.sql.SQLException;
import org.testng.Assert;

/**
 * UI checkout → harici API senkronu → SQL Server kalıcılığı zincirini yürütür.
 * LambdaTest playground kendi backend'imize yazmaz; E2E köprüsü 3 katmanı tek akışta birleştirir.
 */
public final class E2EOrderFlow {
    private E2EOrderFlow() {
    }

    public static String buildOrderNo(String caseId) {
        return "ORD-" + caseId + "-" + System.currentTimeMillis();
    }

    public static UiCheckoutResult runUiCheckout(E2EScenario scenario) {
        String storeHome = ConfigManager.get("store.home.url");
        if (storeHome == null || storeHome.isBlank()) {
            storeHome = "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        DriverFactory.getDriver().get(storeHome);

        CartPage cartPage = new CartPage();
        String product = scenario.getProductNameContains();
        SearchPage searchPage = new SearchPage();
        searchPage.searchFromHeader(product);

        boolean added = cartPage.addProductToCartByNameFragment(product);
        if (!added) {
            added = cartPage.addProductToCartFromDetailWithQuantity(product, 1);
        }
        Assert.assertTrue(
                added,
                "E2E UI: sepete ürün eklenemedi — case " + scenario.getCaseId() + " product=" + product);

        String alert = cartPage.waitForCartAlertText().toLowerCase();
        if (!alert.isBlank()) {
            Assert.assertTrue(
                    alert.contains("success") || alert.contains("cart"),
                    "E2E UI: sepete ekleme geri bildirimi bekleniyordu — case "
                            + scenario.getCaseId() + " alert=" + alert);
        }

        cartPage.openCartPage();
        Assert.assertTrue(
                cartPage.visibleBodyTextLower().contains("checkout"),
                "E2E UI: cart sayfasında checkout aksiyonu görünmeli — case " + scenario.getCaseId());

        CheckoutPage checkoutPage = new CheckoutPage();
        checkoutPage.openCheckoutPage();
        String url = checkoutPage.currentUrlLower();
        Assert.assertTrue(
                url.contains("route=checkout/checkout") || url.contains("route=checkout/cart"),
                "E2E UI: checkout akışına ulaşılamadı — case " + scenario.getCaseId() + " url=" + url);

        System.out.println("[E2E-UI-PASS] " + scenario.getCaseId() + " checkoutUrl=" + url);
        return new UiCheckoutResult(product, url);
    }

    public static void runApiOrderSync(E2EScenario scenario, String orderNo, String customerEmail, String product) {
        if (!ConfigManager.getBoolean("run.api.layer", false)) {
            throw new AssertionError("E2E API adımı için run.api.layer=true gerekli");
        }
        if (!ApiClient.isApiKeyConfigured()) {
            Assert.fail("E2E API: api.reqres.api.key tanımlı değil");
        }

        ApiClient.configureBaseUri();
        Response response = ApiClient.syncOrderForE2E(orderNo, customerEmail, product, scenario.getCaseId());

        Assert.assertEquals(
                response.statusCode(),
                scenario.getExpectedApiStatus(),
                "E2E API: sipariş senkronu HTTP durumu — case " + scenario.getCaseId()
                        + " body=" + abbreviate(response.getBody().asString(), 200));

        JsonPath json = response.jsonPath();
        String returnedName = json.getString("name");
        Assert.assertEquals(
                returnedName,
                orderNo,
                "E2E API: reqres yanıtı orderNo ile eşleşmeli — case " + scenario.getCaseId());

        Assert.assertNotNull(
                json.get("createdAt"),
                "E2E API: createdAt alanı bekleniyordu — case " + scenario.getCaseId());

        System.out.println("[E2E-API-PASS] " + scenario.getCaseId() + " orderNo=" + orderNo);
    }

    public static void runDbPersistence(
            E2EScenario scenario,
            String orderNo,
            String product,
            String customerEmail,
            String checkoutUrl) {
        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {
            throw new AssertionError("E2E DB adımı için run.db.ui.verify=true gerekli");
        }
        if (!DbClient.isSqlServerAvailable()) {
            Assert.fail("E2E DB: SQL Server TestDb erişilemedi — SSMS kurulumu ve config.properties kontrol edin");
        }

        String status = scenario.getExpectedDbStatus();
        try {
            DbClient.insertCheckoutOrder(
                    orderNo,
                    scenario.getCaseId(),
                    product,
                    customerEmail,
                    checkoutUrl,
                    status);
        } catch (SQLException e) {
            Assert.fail("E2E DB: insert failed — case " + scenario.getCaseId() + ": " + e.getMessage());
        }

        Assert.assertTrue(
                DbClient.orderExists(orderNo),
                "E2E DB: sipariş SQL Server'da bulunmalı — case " + scenario.getCaseId() + " orderNo=" + orderNo);

        Assert.assertEquals(
                DbClient.getOrderStatus(orderNo),
                status,
                "E2E DB: sipariş durumu uyuşmuyor — case " + scenario.getCaseId());

        System.out.println("[E2E-DB-PASS] " + scenario.getCaseId()
                + " orderNo=" + orderNo
                + " status=" + status);
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public record UiCheckoutResult(String productName, String checkoutUrl) {
    }
}
