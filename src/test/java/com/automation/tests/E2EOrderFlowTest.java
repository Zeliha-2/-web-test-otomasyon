package com.automation.tests;

import com.automation.config.ConfigManager;
import com.automation.db.DbClient;
import com.automation.models.E2EScenario;
import com.automation.utils.ApiClient;
import com.automation.utils.E2EOrderFlow;
import com.automation.utils.JsonDataLoader;
import java.lang.reflect.Method;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Gerçek 3 katmanlı E2E: UI (Selenium checkout) → API (reqres order sync) → DB (SQL Server orders).
 */
public class E2EOrderFlowTest extends MyAccountAuthenticatedBaseTest {

    @Override
    protected String navigationUrl() {
        String url = ConfigManager.get("store.home.url");
        if (url == null || url.isBlank()) {
            return "https://ecommerce-playground.lambdatest.io/index.php?route=common/home";
        }
        return url;
    }

    @BeforeMethod(alwaysRun = true)
    public void guardE2ELayers(Method method) {
        if (!ConfigManager.getBoolean("run.e2e.layer", true)) {
            throw new SkipException("run.e2e.layer=false — E2E katmanı devre dışı");
        }
        if (!ConfigManager.getBoolean("run.ui.layer", true)) {
            throw new SkipException("run.ui.layer=false — E2E UI adımı devre dışı");
        }
        if (!ConfigManager.getBoolean("run.api.layer", false)) {
            throw new SkipException("run.api.layer=false — E2E API adımı devre dışı");
        }
        if (!ConfigManager.getBoolean("run.db.ui.verify", false)) {
            throw new SkipException("run.db.ui.verify=false — E2E DB adımı devre dışı");
        }
        if (!ApiClient.isApiKeyConfigured()) {
            throw new SkipException(
                    "api.reqres.api.key eksik — E2E API adımı atlandı (https://app.reqres.in/api-keys)");
        }
        if (!DbClient.isSqlServerAvailable()) {
            throw new SkipException(
                    "SQL Server TestDb erişilemedi — E2E DB adımı atlandı (setup_testdb.sql + config.properties)");
        }
        System.out.println("[E2E-TEST-START] " + method.getName());
    }

    @DataProvider(name = "e2eScenarios")
    public Object[][] e2eScenarios(Method method) {
        E2EScenario[] scenarios = JsonDataLoader.load("testdata/e2e-scenarios.json", E2EScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(
            dataProvider = "e2eScenarios",
            description = "UI checkout → API order sync → SQL Server persistence")
    public void runE2EOrderFlow(E2EScenario scenario) {
        String orderNo = E2EOrderFlow.buildOrderNo(scenario.getCaseId());
        String customerEmail = getCachedEmail();

        E2EOrderFlow.UiCheckoutResult ui = E2EOrderFlow.runUiCheckout(scenario);

        E2EOrderFlow.runApiOrderSync(scenario, orderNo, customerEmail, ui.productName());

        E2EOrderFlow.runDbPersistence(
                scenario,
                orderNo,
                ui.productName(),
                customerEmail,
                ui.checkoutUrl());

        System.out.println("[E2E-PASS] " + scenario.getCaseId()
                + " orderNo=" + orderNo
                + " email=" + customerEmail);
    }
}
