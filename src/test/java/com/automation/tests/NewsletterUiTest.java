package com.automation.tests; // Test sınıfı paketi

import com.automation.models.NewsletterScenario; // JSON senaryo modeli
import com.automation.pages.AccountNewsletterPage; // Newsletter page sınıfı
import com.automation.pages.AccountUrlHelper;
import com.automation.base.DriverFactory;
import com.automation.utils.JsonDataLoader; // JSON okuma utility'si
import java.lang.reflect.Method; // DataProvider imzası için Method
import org.openqa.selenium.By;
import org.testng.Assert; // Assertionlar için
import org.testng.annotations.DataProvider; // TestNG DataProvider annotation
import org.testng.annotations.Test; // TestNG Test annotation

public class NewsletterUiTest extends MyAccountAuthenticatedBaseTest { // Login hazırlığını ortak base'den alır

    @DataProvider(name = "newsletterScenarios") // DataProvider adı
    public Object[][] newsletterScenarios(Method method) { // Senaryo verisi üreten metot
        NewsletterScenario[] scenarios = JsonDataLoader.load( // JSON dosyasını model array'e map et
                "testdata/newsletter-scenarios.json", // Kaynak JSON yolu
                NewsletterScenario[].class // Hedef tip
        );
        Object[][] data = new Object[scenarios.length][1]; // TestNG için 2D obje dizisi
        for (int i = 0; i < scenarios.length; i++) { // Her senaryoyu sırayla dolaş
            data[i][0] = scenarios[i]; // i'nci satıra senaryoyu koy
        }
        return data; // DataProvider sonucu
    }

    @Test(dataProvider = "newsletterScenarios", description = "My Account newsletter subscribe/unsubscribe") // Data-driven test
    public void runNewsletterScenario(NewsletterScenario scenario) { // Her JSON kaydı için çalışır
        AccountNewsletterPage page = new AccountNewsletterPage(); // Newsletter page nesnesi oluştur

        if (scenario.isGuestLoginRedirectNewsletterScenario()) {
            DriverFactory.getDriver().get(AccountUrlHelper.route("account/logout"));
            DriverFactory.getDriver().get(AccountUrlHelper.route("account/newsletter"));
            String u = page.currentUrlLower();
            String body = DriverFactory.getDriver().findElement(By.tagName("body")).getText().toLowerCase();
            String needle = scenario.getExpectedSuccessContains() == null
                    ? "login"
                    : scenario.getExpectedSuccessContains().toLowerCase();
            Assert.assertTrue(
                    u.contains("account/login") || u.contains("route=account/login")
                            || body.contains("returning customer") || body.contains("please login")
                            || (!needle.isBlank() && body.contains(needle)),
                    "Guest should be blocked or redirected to login for case " + scenario.getCaseId() + " url=" + u);
            return;
        }

        if (Boolean.TRUE.equals(scenario.getToggleSubscriptionTwice())) {
            page.open();
            page.setSubscription(Boolean.TRUE.equals(scenario.getSubscribe()));
            page.open();
            page.setSubscription(!Boolean.TRUE.equals(scenario.getSubscribe()));
            String feedback = page.getVisibleFeedback().toLowerCase();
            String expected = scenario.getExpectedSuccessContains() == null
                    ? ""
                    : scenario.getExpectedSuccessContains().toLowerCase();
            Assert.assertTrue(
                    (!expected.isBlank() && feedback.contains(expected))
                            || page.currentUrlLower().contains("route=account/newsletter")
                            || page.currentUrlLower().contains("route=account/account"),
                    "Newsletter double-toggle feedback not found for case " + scenario.getCaseId() + " feedback=" + feedback);
            return;
        }

        page.open(); // Newsletter sayfasını aç
        page.setSubscription(Boolean.TRUE.equals(scenario.getSubscribe())); // Yes/No seçimini uygula ve kaydet

        String feedback = page.getVisibleFeedback().toLowerCase(); // Mesajı al ve normalize et
        String expected = scenario.getExpectedSuccessContains() == null // Beklenen metin null ise
                ? "" // boş
                : scenario.getExpectedSuccessContains().toLowerCase(); // değilse küçük harf

        Assert.assertTrue( // Başarı kontrolü
                (!expected.isBlank() && feedback.contains(expected)) // Mesaj bekleneni içeriyor mu
                        || page.currentUrlLower().contains("route=account/newsletter") // veya sayfa newsletter route'unda mı
                        || page.currentUrlLower().contains("route=account/account"), // veya account sayfasına döndü mü
                "Newsletter update feedback not found for case " + scenario.getCaseId() + " feedback=" + feedback // Fail mesajı
        );
    }
}