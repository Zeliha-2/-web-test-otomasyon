package com.automation.utils;

import org.testng.ITestResult;

public final class UiEvidence {

    private UiEvidence() {
    }

    public static void recordVisibleMessage(ITestResult result, String message, String pageUrl) {
        if (result == null) {
            return;
        }
        if (message != null && !message.isBlank()) {
            result.setAttribute("visibleUiMessage", message.trim());
        }
        if (pageUrl != null && !pageUrl.isBlank()) {
            result.setAttribute("failurePageUrl", pageUrl.trim());
            result.setAttribute("failurePageLabel", pageLabel(pageUrl));
        }
    }

    public static String pageLabel(String url) {
        if (url == null || url.isBlank()) {
            return "Uygulama sayfası";
        }
        if (url.contains("route=account/forgotten")) {
            return "Şifremi Unuttum sayfası";
        }
        if (url.contains("route=account/login")) {
            return "Giriş (Login) sayfası";
        }
        if (url.contains("route=account/register")) {
            return "Kayıt (Register) sayfası";
        }
        if (url.contains("route=checkout")) {
            return "Ödeme (Checkout) sayfası";
        }
        if (url.contains("route=account/")) {
            return "Hesabım sayfası";
        }
        return "Uygulama sayfası";
    }

    public static String pageLabelDative(String url) {
        String label = pageLabel(url);
        if (label.endsWith("sayfası")) {
            return label.substring(0, label.length() - "sayfası".length()) + "sayfasına";
        }
        return label + "na";
    }
}
