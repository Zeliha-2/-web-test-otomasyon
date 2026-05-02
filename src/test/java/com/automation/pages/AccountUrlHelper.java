package com.automation.pages;

import com.automation.config.ConfigManager;

public final class AccountUrlHelper {
    private AccountUrlHelper() {
    }

    public static String indexPhpBase() {
        String login = ConfigManager.get("base.url");
        if (login == null || login.isBlank()) {
            login = "https://ecommerce-playground.lambdatest.io/index.php?route=account/login";
        }
        int q = login.indexOf('?');
        return q >= 0 ? login.substring(0, q) : login;
    }

    public static String route(String routeQueryValue) {
        return indexPhpBase() + "?route=" + routeQueryValue;
    }
}
