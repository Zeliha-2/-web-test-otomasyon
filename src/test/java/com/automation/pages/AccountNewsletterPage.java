package com.automation.pages; // Bu sınıf page katmanında

import com.automation.base.DriverFactory; // Aktif driver'ı almak için
import com.automation.config.ConfigManager; // Wait süresini config'den okumak için
import java.time.Duration; // Wait için süre tipi
import org.openqa.selenium.By; // Locator sınıfı
import org.openqa.selenium.JavascriptExecutor; // Güvenli click/scroll için JS executor
import org.openqa.selenium.WebDriver; // WebDriver tipi
import org.openqa.selenium.WebElement; // WebElement tipi
import org.openqa.selenium.support.ui.ExpectedConditions; // Explicit wait koşulları
import org.openqa.selenium.support.ui.WebDriverWait; // Explicit wait sınıfı

public class AccountNewsletterPage { // Newsletter ekran aksiyonlarını kapsüller
    private final WebDriver driver; // Sınıf içinde kullanılacak driver referansı

    public AccountNewsletterPage() { // Constructor
        this.driver = DriverFactory.getDriver(); // Aktif thread driver'ı al
    }

    public void open() { // Newsletter sayfasını açan metot
        driver.get(AccountUrlHelper.route("account/newsletter")); // OpenCart newsletter route'una git
    }

    public void setSubscription(boolean subscribe) { // Yes/No seçimi yapan metot
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8); // Config'den wait süresi al
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec)); // Explicit wait oluştur

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("content"))); // İçerik alanı yüklenene kadar bekle

        // Tema farkları için hem input hem id hem de label fallback locator'ları hazırlanır.
        String value = subscribe ? "1" : "0"; // true ise Yes(1), false ise No(0)
        String id = subscribe ? "input-newsletter-yes" : "input-newsletter-no"; // OpenCart'ta sık görülen id'ler
        By[] candidates = new By[] {
                By.cssSelector("input[name='newsletter'][value='" + value + "']"), // En genel form
                By.id(id), // ID ile erişim
                By.cssSelector("label[for='" + id + "']") // Input gizliyse label'a tıklama
        };

        boolean clicked = false; // Herhangi bir fallback başarılı mı?
        for (By candidate : candidates) {
            try {
                WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(candidate)); // Önce varlığını bekle
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el); // Görünür alana getir
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el); // JS click (overlay durumlarında daha güvenli)
                clicked = true;
                break;
            } catch (Exception ignored) {
                // Sonraki fallback locator denenecek.
            }
        }
        if (!clicked) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "Newsletter subscribe/unsubscribe control not found for value=" + value);
        }

        By submitBy = By.cssSelector("#content input.btn-primary[type='submit'], #content button[type='submit'], #content input[value='Continue']"); // Continue/Save butonu
        WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(submitBy)); // Buton tıklanabilir olana kadar bekle
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submit); // Butonu görünür alana getir
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit); // Butona JS ile tıkla
    }

    public String getVisibleFeedback() { // Sayfadan başarı/hata mesajını okuyan metot
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8); // Wait süresini al
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec)); // Wait nesnesi oluştur
        try { // Mesaj bulunamazsa patlamaması için try/catch
            WebElement msg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".alert-success, .alert.alert-success, .alert-danger, .alert.alert-danger"))); // Uygun alert locator'ları
            return msg.getText().trim(); // Mesajı kırpıp döndür
        } catch (Exception e) { // Mesaj bulunamazsa
            return ""; // Boş string döndür
        }
    }

    public String currentUrlLower() { // URL doğrulaması için yardımcı metot
        return driver.getCurrentUrl().toLowerCase(); // URL'i küçük harf dön
    }
}