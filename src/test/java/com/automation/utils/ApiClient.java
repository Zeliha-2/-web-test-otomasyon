package com.automation.utils;

import com.automation.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.Optional;

public final class ApiClient {
    private ApiClient() {
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
            Response response = RestAssured.given()
                    .baseUri(ConfigManager.get("api.base.url"))
                    .when()
                    .get("/api/users?page=2")
                    .andReturn();
            return Optional.of(response.statusCode());
        } catch (Exception e) {
            System.out.println("[API-WARN] API check skipped: " + e.getMessage());
            return Optional.empty();
        }
    }
}
