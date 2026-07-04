package com.automation.utils;

import com.automation.config.ConfigManager;
import com.automation.models.ApiScenario;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Optional;

public final class ApiClient {
    private static final String DEFAULT_USER_AGENT = "AI-3Layer-TestFramework/1.0";

    private ApiClient() {
    }

    public static void configureBaseUri() {
        String base = ConfigManager.get("api.base.url");
        if (base == null || base.isBlank()) {
            base = "https://reqres.in";
        }
        RestAssured.baseURI = base.replaceAll("/+$", "");
    }

    public static Response execute(ApiScenario scenario) {
        RequestSpecification spec = RestAssured.given()
                .relaxedHTTPSValidation()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        applyReqresAuth(spec, scenario);

        String body = scenario.getRequestBody();
        if (body != null && !body.isBlank()) {
            spec.body(body);
        }

        String method = scenario.getMethod() == null ? "GET" : scenario.getMethod().trim().toUpperCase();
        String endpoint = scenario.getEndpoint() == null ? "/" : scenario.getEndpoint();

        return switch (method) {
            case "POST" -> spec.post(endpoint);
            case "PUT" -> spec.put(endpoint);
            case "PATCH" -> spec.patch(endpoint);
            case "DELETE" -> spec.delete(endpoint);
            default -> spec.get(endpoint);
        };
    }

    public static Response getUsersPage2() {
        return RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .when()
                .get("/api/users?page=2")
                .then()
                .extract()
                .response();
    }

    public static Optional<Integer> getUsersPage2StatusCodeSafe() {
        try {
            configureBaseUri();
            Response response = RestAssured.given()
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("x-api-key", resolveApiKey())
                    .when()
                    .get("/api/users?page=2")
                    .andReturn();
            return Optional.of(response.statusCode());
        } catch (Exception e) {
            System.out.println("[API-WARN] API check skipped: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isApiKeyConfigured() {
        String key = resolveApiKey();
        return key != null && !key.isBlank();
    }

    /**
     * E2E akışında UI checkout sonrası harici sipariş servisine senkron simülasyonu.
     * reqres.in POST /api/users — name=orderNo, job=product+caseId.
     */
    public static Response syncOrderForE2E(
            String orderNo,
            String customerEmail,
            String productName,
            String caseId) {
        configureBaseUri();
        String safeProduct = productName == null ? "unknown" : productName.replace("\"", "'");
        String safeCase = caseId == null ? "E2E" : caseId.replace("\"", "'");
        String body = "{"
                + "\"name\":\"" + orderNo + "\","
                + "\"job\":\"order-sync|" + safeProduct + "|" + safeCase + "|" + customerEmail + "\""
                + "}";
        return RestAssured.given()
                .relaxedHTTPSValidation()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("x-api-key", resolveApiKey())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .post("/api/users");
    }

    private static void applyReqresAuth(RequestSpecification spec, ApiScenario scenario) {
        String apiKey = resolveApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            spec.header("x-api-key", apiKey.trim());
        }
        if (!Boolean.TRUE.equals(scenario.getOmitAuthHeader())) {
            String token = ConfigManager.get("api.auth.token");
            if (token != null && !token.isBlank()) {
                spec.header("Authorization", "Bearer " + token.trim());
            }
        }
    }

    private static String resolveApiKey() {
        return ConfigManager.get("api.reqres.api.key");
    }
}
