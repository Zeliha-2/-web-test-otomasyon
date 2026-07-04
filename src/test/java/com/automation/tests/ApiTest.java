package com.automation.tests;

import com.automation.config.ConfigManager;
import com.automation.dashboard.DashboardResultsListener;
import com.automation.models.ApiScenario;
import com.automation.utils.ApiClient;
import com.automation.utils.JsonDataLoader;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(DashboardResultsListener.class)
public class ApiTest {

    private static final int DEFAULT_MAX_RESPONSE_MS = 2000;

    @BeforeClass(alwaysRun = true)
    public void configureRestAssured() {
        if (!ConfigManager.getBoolean("run.api.layer", false)) {
            return;
        }
        ApiClient.configureBaseUri();
    }

    @BeforeMethod(alwaysRun = true)
    public void guardApiLayer(Method method) {
        if (!ConfigManager.getBoolean("run.api.layer", false)) {
            throw new SkipException("run.api.layer=false — API katmanı devre dışı");
        }
        if (!ApiClient.isApiKeyConfigured()) {
            throw new SkipException(
                    "api.reqres.api.key eksik — https://app.reqres.in/api-keys adresinden ücretsiz anahtar alın "
                            + "ve config.properties veya env API_REQRES_API_KEY ile tanımlayın");
        }
        System.out.println("[API-TEST-START] " + method.getName());
    }

    @DataProvider(name = "apiScenarios")
    public Object[][] apiScenarios(Method method) {
        ApiScenario[] scenarios = JsonDataLoader.load("testdata/api-scenarios.json", ApiScenario[].class);
        Object[][] data = new Object[scenarios.length][1];
        for (int i = 0; i < scenarios.length; i++) {
            data[i][0] = scenarios[i];
        }
        return data;
    }

    @Test(
            dataProvider = "apiScenarios",
            description = "Data-driven reqres.in API scenarios (ISTQB + OWASP)")
    public void runApiScenario(ApiScenario scenario) {
        long started = System.currentTimeMillis();
        Response response = ApiClient.execute(scenario);
        long elapsed = System.currentTimeMillis() - started;

        int maxMs = ConfigManager.getInt("api.max.response.ms", DEFAULT_MAX_RESPONSE_MS);

        Assert.assertEquals(
                response.statusCode(),
                scenario.getExpectedStatus(),
                "HTTP status mismatch for case " + scenario.getCaseId()
                        + " [" + scenario.getTechnique() + "] body="
                        + abbreviate(response.getBody().asString(), 300));

        Assert.assertTrue(
                response.getTime() <= maxMs && elapsed <= maxMs + 500,
                "Response time exceeded " + maxMs + "ms for case " + scenario.getCaseId()
                        + " restAssuredTime=" + response.getTime() + "ms elapsed=" + elapsed + "ms");

        assertResponseField(scenario, response);

        System.out.println("[API-PASS] " + scenario.getCaseId()
                + " | " + scenario.getMethod() + " " + scenario.getEndpoint()
                + " | " + scenario.getTechnique()
                + " | " + response.getTime() + "ms");
    }

    private static void assertResponseField(ApiScenario scenario, Response response) {
        String field = scenario.getExpectedResponseField();
        if (field == null || field.isBlank()) {
            return;
        }
        String body = response.getBody().asString();
        if (body == null || body.isBlank()) {
            Assert.fail("Expected JSON field '" + field + "' but response body was empty for case "
                    + scenario.getCaseId());
        }

        Object actual = JsonPath.from(body).get(field);
        String expected = scenario.getExpectedResponseValue();

        if (expected == null || expected.isBlank() || "*".equals(expected.trim())) {
            Assert.assertNotNull(actual,
                    "Expected field '" + field + "' to exist for case " + scenario.getCaseId());
            if (actual instanceof String s) {
                Assert.assertFalse(s.isBlank(),
                        "Expected field '" + field + "' to be non-empty for case " + scenario.getCaseId());
            }
            return;
        }

        Assert.assertEquals(
                String.valueOf(actual),
                expected,
                "Field '" + field + "' value mismatch for case " + scenario.getCaseId());
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
