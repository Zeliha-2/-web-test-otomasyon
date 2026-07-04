package com.automation.dashboard;



import com.automation.config.ConfigManager;

import com.automation.utils.UiEvidence;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.Locale;

import java.util.Map;



public final class JiraBugReportBuilder {

    private static final DateTimeFormatter TR_DATE =

            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.forLanguageTag("tr"));



    private JiraBugReportBuilder() {

    }



    public static JiraBugReport build(DashboardTestEntry entry, String runDateLabel) {

        JiraBugReport report = new JiraBugReport();

        report.projectKey = safe(ConfigManager.get("jira.project.key"), "QA");

        report.issueType = safe(ConfigManager.get("jira.issue.type"), "Bug");

        report.channel = safe(ConfigManager.get("jira.channel"), "Web");

        report.qaOrigin = safe(ConfigManager.get("jira.qa.origin"), "Test Otomasyon");

        report.qaProcess = safe(ConfigManager.get("jira.qa.process"), "Regression");

        report.caseId = safe(entry.caseId, "—");

        report.module = safe(entry.module, "Unknown");

        report.technique = safe(entry.technique, "—");

        report.standard = safe(entry.standard, "—");

        report.screenshotPath = safe(entry.screenshotPath, "");

        report.errorLogPath = safe(entry.errorLogPath, "");

        report.attachmentFileName = buildLogFileName(entry);

        report.detectedAt = LocalDateTime.now().format(TR_DATE);

        report.testSteps = buildTestSteps(entry);

        report.expectedResult = buildExpectedResult(entry);

        report.actualResult = buildActualResult(entry);

        assignSeverityAndPriority(report, entry);

        report.summary = buildSummary(entry);

        report.description = buildDescription(report, entry);

        return report;

    }



    private static String buildSummary(DashboardTestEntry entry) {

        String fromContext = summaryFromModule(entry);

        if (fromContext != null) {

            return shortenPlain(fromContext, 120);

        }

        if (entry.description != null && !entry.description.isBlank() && isTurkish(entry.description)) {

            return shortenPlain(entry.description.trim(), 120);

        }

        return shortenPlain(humanizeError(entry.errorMessage), 120);

    }



    private static String summaryFromModule(DashboardTestEntry entry) {

        String module = safe(entry.module, "");

        Map<String, String> ctx = entry.scenarioContext == null ? Map.of() : entry.scenarioContext;

        String msg = safe(entry.errorMessage, "").toLowerCase(Locale.ROOT);



        if ("ForgotPassword".equals(module)) {

            String email = ctx.getOrDefault("email", "e-posta");

            if (msg.contains("expected success info not found") || msg.contains("confirmation link")) {

                return "Şifremi unuttum: " + email + " için yanlış bilgilendirme mesajı gösteriliyor";

            }

            return "Şifremi unuttum akışında " + email + " senaryosu beklenen sonucu vermedi";

        }

        if ("Login".equals(module)) {

            if (msg.contains("warning") || msg.contains("uyarı")) {

                return "Giriş ekranında beklenen uyarı mesajı görüntülenmedi";

            }

            return "Giriş akışında kullanıcı doğrulama hatası oluştu";

        }

        if (msg.contains("auth bypass") || msg.contains("login gerektirmeden")) {

            return "Giriş yapılmadan " + moduleFriendly(module) + " sayfasına erişilebiliyor";

        }

        if (msg.contains("could not add product") || msg.contains("sepete")) {

            return moduleFriendly(module) + " akışında ürün sepete eklenemedi";

        }

        if (msg.contains("checkout") || msg.contains("ödeme")) {

            return "Ödeme adımında beklenen ekran veya davranış oluşmadı";

        }

        if (msg.contains("timeout") || msg.contains("timed out")) {

            return moduleFriendly(module) + " ekranında işlem zaman aşımına uğradı";

        }

        if (entry.description != null && !entry.description.isBlank()) {

            return turkishDescriptionSummary(entry.description.trim(), module);

        }

        return moduleFriendly(module) + " testinde beklenmeyen bir hata oluştu";

    }



    private static String turkishDescriptionSummary(String description, String module) {

        String lower = description.toLowerCase(Locale.ROOT);

        if (lower.contains("admin email") || lower.contains("common admin")) {

            return "Şifremi unuttum: yaygın admin e-posta denemesinde güvenlik mesajı hatalı";

        }

        if (lower.contains("invalid password") || lower.contains("geçersiz şifre")) {

            return "Giriş: geçersiz şifre denemesinde beklenen uyarı gösterilmedi";

        }

        if (isTurkish(description)) {

            return description;

        }

        return moduleFriendly(module) + " senaryosunda hata oluştu";

    }



    private static String buildExpectedResult(DashboardTestEntry entry) {

        String module = safe(entry.module, "");

        Map<String, String> ctx = entry.scenarioContext == null ? Map.of() : entry.scenarioContext;



        if ("ForgotPassword".equals(module)) {

            return buildForgotPasswordExpected(ctx);

        }

        if ("Login".equals(module)) {

            return buildLoginExpected(ctx);

        }



        if (entry.description != null && !entry.description.isBlank() && isTurkish(entry.description)) {

            String desc = entry.description.trim();

            return desc.endsWith(".") ? desc : desc + ".";

        }

        return moduleFriendly(safe(entry.module, "İlgili")) + " bölümünde tanımlı senaryo adımları sorunsuz tamamlanmalı "

                + "ve kullanıcı ekranda beklenen mesajı veya davranışı görmelidir.";

    }



    private static String buildForgotPasswordExpected(Map<String, String> ctx) {

        String email = ctx.getOrDefault("email", "(test e-postası)");

        String expectedMsg = ctx.getOrDefault("expectedMessageContains", "");

        boolean expectSuccess = "true".equalsIgnoreCase(ctx.get("expectSuccess"));



        if (expectSuccess && expectedMsg.toLowerCase(Locale.ROOT).contains("not found")) {

            return "Güvenlik gereği, \"" + email + "\" girildiğinde sistem, adresin kayıtlı olup olmadığına "

                    + "bakılmaksızın aynı genel mesajı göstermelidir: e-posta kayıtlarda bulunamadı "

                    + "(\"" + expectedMsg + "\" anlamına gelen ifade). "

                    + "«E-posta gönderildi» veya onay bağlantısı mesajı gösterilmemelidir.";

        }

        if (expectSuccess && !expectedMsg.isBlank()) {

            return "Kullanıcı \"" + email + "\" girdikten sonra ekranda \"" + expectedMsg + "\" "

                    + "içeren bilgilendirme mesajı görünmelidir.";

        }

        if (!expectSuccess && !expectedMsg.isBlank()) {

            return "Geçersiz veya hatalı e-posta (\"" + email + "\") girildiğinde \"" + expectedMsg + "\" "

                    + "içeren uyarı görünmeli veya kullanıcı şifremi unuttum sayfasında kalmalıdır.";

        }

        return "Şifremi unuttum akışı, girilen e-posta için doğru bilgilendirme mesajını göstermelidir.";

    }



    private static String buildLoginExpected(Map<String, String> ctx) {

        String email = ctx.getOrDefault("email", "(test kullanıcısı)");

        String expected = ctx.getOrDefault("expectedWarningContains", "");

        boolean expectWarning = "true".equalsIgnoreCase(ctx.get("expectWarning"));



        if (expectWarning && !expected.isBlank()) {

            return "Geçersiz giriş denemesinde (\"" + email + "\") ekranda \"" + expected + "\" "

                    + "içeren uyarı mesajı görünmelidir. Kullanıcı hesaba giriş yapmamalıdır.";

        }

        return "Geçerli kullanıcı bilgileri ile (\"" + email + "\") giriş başarılı olmalı "

                + "ve kullanıcı hesap alanına yönlendirilmelidir.";

    }



    private static String buildActualResult(DashboardTestEntry entry) {
        if ("ForgotPassword".equals(safe(entry.module, ""))) {
            return buildForgotPasswordActual(entry);
        }
        return humanizeError(entry.errorMessage);
    }

    private static String buildForgotPasswordActual(DashboardTestEntry entry) {
        Map<String, String> ctx = entry.scenarioContext == null ? Map.of() : entry.scenarioContext;
        String email = ctx.getOrDefault("email", "(girilen e-posta)");
        String visible = ctx.get("visibleUiMessage");
        String page = UiEvidence.pageLabelDative(ctx.get("failurePageUrl"));
        if (page == null || page.isBlank()) {
            page = toDativePage(ctx.getOrDefault("failurePageLabel", "uygulama sayfası"));
        }

        if (visible != null && !visible.isBlank()) {
            return """
                    «%s» adresi şifremi unuttum formuna girilip gönderildikten sonra %s yönlendirildi.

                    Sayfanın en üstündeki yeşil bilgilendirme kutusunda (ekran görüntüsüne bakın) şu metin görüntülendi:
                    «%s»"""
                    .formatted(email, page, visible)
                    .trim();
        }
        return humanizeError(entry.errorMessage);
    }

    private static String toDativePage(String page) {
        if (page == null || page.isBlank()) {
            return "uygulama sayfasına";
        }
        if (page.endsWith("sayfası")) {
            return page.substring(0, page.length() - "sayfası".length()) + "sayfasına";
        }
        return page + "na";
    }



    private static String buildTestSteps(DashboardTestEntry entry) {

        String module = safe(entry.module, "");

        if ("ForgotPassword".equals(module)) {

            return buildForgotPasswordSteps(entry.scenarioContext);

        }

        if ("Login".equals(module)) {

            return buildLoginSteps(entry.scenarioContext);

        }

        if ("Register".equals(module)) {

            return buildRegisterSteps(entry);

        }

        if (isApiLayer(entry)) {

            return buildApiSteps(entry);

        }

        if (isDbLayer(entry)) {

            return buildDbSteps(entry);

        }

        return buildGenericUiSteps(entry);

    }



    private static String buildForgotPasswordSteps(Map<String, String> ctx) {

        Map<String, String> c = ctx == null ? Map.of() : ctx;

        String email = c.getOrDefault("email", "(senaryoda tanımlı e-posta)");

        return """

                1. Uygulamanın giriş (Login) sayfası açılır.

                2. "Şifremi Unuttum" (Forgotten Password) bağlantısına tıklanır.

                3. Açılan formda e-posta alanına "%s" adresi yazılır.

                4. Gönder / Continue butonuna basılır.

                5. Sayfada görünen bilgilendirme veya hata mesajı okunur.

                6. Mesajın, test senaryosunda tanımlanan beklenti ile uyumlu olup olmadığı kontrol edilir."""

                .formatted(email)

                .trim();

    }



    private static String buildLoginSteps(Map<String, String> ctx) {

        Map<String, String> c = ctx == null ? Map.of() : ctx;

        String email = c.getOrDefault("email", "(senaryoda tanımlı e-posta)");

        String password = c.getOrDefault("password", "(senaryoda tanımlı şifre)");

        return """

                1. Uygulamanın giriş (Login) sayfası açılır.

                2. E-posta alanına "%s" yazılır.

                3. Şifre alanına "%s" yazılır.

                4. Giriş (Login) butonuna tıklanır.

                5. Yönlendirme, uyarı mesajı veya hata metni kontrol edilir."""

                .formatted(email, password)

                .trim();

    }



    private static String buildRegisterSteps(DashboardTestEntry entry) {

        return """

                1. Uygulamanın kayıt (Register) sayfası açılır.

                2. Senaryoda tanımlı kullanıcı bilgileri forma girilir.

                3. Kayıt ol butonuna tıklanır.

                4. Başarı, uyarı veya doğrulama mesajı kontrol edilir."""

                .trim();

    }



    private static String buildApiSteps(DashboardTestEntry entry) {

        return """

                1. Test ortamı ve kimlik bilgileri hazırlanır.

                2. Senaryoda tanımlı HTTP isteği ilgili API uç noktasına gönderilir.

                3. Yanıt kodu (status code) kontrol edilir.

                4. Yanıt gövdesindeki (body) alanlar beklenen değerlerle karşılaştırılır."""

                .trim();

    }



    private static String buildDbSteps(DashboardTestEntry entry) {

        return """

                1. Test veritabanı bağlantısı doğrulanır.

                2. Senaryoda tanımlı SQL sorgusu veya veri işlemi çalıştırılır.

                3. Dönen kayıt sayısı ve alan değerleri kontrol edilir.

                4. Sonuç, senaryodaki beklenti ile karşılaştırılır."""

                .trim();

    }



    private static String buildGenericUiSteps(DashboardTestEntry entry) {

        String module = moduleFriendly(safe(entry.module, "uygulama"));

        String scenarioNote = turkishScenarioNote(entry.description);

        StringBuilder sb = new StringBuilder();

        sb.append("1. Test ortamı açılır ve uygulama erişilebilir hale getirilir.\n");

        sb.append("2. ").append(module).append(" bölümüne gidilir.\n");

        if (scenarioNote != null) {

            sb.append("3. ").append(scenarioNote).append("\n");

            sb.append("4. Ekranda görünen sonuç ve mesajlar kontrol edilir.\n");

            sb.append("5. Sonuç, test senaryosundaki beklenti ile karşılaştırılır.");

        } else {

            sb.append("3. ").append(module).append(" için tanımlı test senaryosu adım adım uygulanır.\n");

            sb.append("4. Ekranda görünen sonuç kontrol edilir.\n");

            sb.append("5. Sonuç, test senaryosundaki beklenti ile karşılaştırılır.");

        }

        return sb.toString().trim();

    }



    private static String turkishScenarioNote(String description) {

        if (description == null || description.isBlank()) {

            return null;

        }

        String lower = description.toLowerCase(Locale.ROOT);

        if (lower.contains("common admin email")) {

            return "Sık kullanılan admin e-posta adresi (ör. admin@admin.com) ile şifre sıfırlama denenir.";

        }

        if (lower.contains("plus-addressing")) {

            return "Artı işaretli e-posta formatı (plus-addressing) ile form doldurulur.";

        }

        if (lower.contains("empty cart")) {

            return "Sepet boşken ödeme adımına geçilmeye çalışılır.";

        }

        if (isTurkish(description)) {

            return description.endsWith(".") ? description : description + ".";

        }

        return "Senaryoda tanımlı kullanıcı adımları uygulanır.";

    }



    /** Jira açıklama alanı: yalnızca meta bilgi (adımlar/sonuçlar ayrı alanlarda). */

    private static String buildDescription(JiraBugReport report, DashboardTestEntry entry) {

        StringBuilder sb = new StringBuilder();

        sb.append("Senaryo Kodu: ").append(report.caseId).append('\n');

        sb.append("Modül: ").append(moduleFriendly(report.module)).append('\n');

        sb.append("Test Tekniği: ").append(techniqueFriendly(report.technique)).append('\n');

        sb.append("Standart: ").append(report.standard).append('\n');

        sb.append("Kanal: ").append(report.channel).append(" · Süreç: ").append(report.qaProcess).append('\n');

        sb.append("Hata Tarihi: ").append(report.detectedAt);

        return sb.toString().trim();

    }



    private static String humanizeError(String raw) {

        if (raw == null || raw.isBlank()) {

            return "Test sırasında beklenmeyen bir hata oluştu; ayrıntılı mesaj kaydedilmedi.";

        }

        String msg = raw.trim();

        String lower = msg.toLowerCase(Locale.ROOT);



        if (lower.contains("expected success info not found")) {

            String screenMsg = extractAfter(msg, "msg=");

            if (screenMsg.toLowerCase(Locale.ROOT).contains("confirmation link")

                    || screenMsg.toLowerCase(Locale.ROOT).contains("email with")) {

                return "Giriş (Login) sayfasının üstündeki yeşil bilgilendirme kutusunda e-posta gönderildi mesajı görüntülendi. "
                        + "Ekrandaki tam metin test kaydında ve ekran görüntüsünde yer almaktadır.";

            }

            return "Şifremi unuttum ekranında beklenen bilgilendirme mesajı görülmedi. "

                    + "Ekranda görünen mesaj: " + shortenPlain(screenMsg.isBlank() ? msg : screenMsg, 200);

        }

        if (lower.contains("auth bypass") || lower.contains("login gerektirmeden")) {

            return "Kullanıcı giriş yapmadan korumalı sayfaya erişebildi. Bu durum yetkisiz erişim riski oluşturur.";

        }

        if (lower.contains("could not add product")) {

            return "Test sırasında ürün sepete eklenemedi; bu nedenle sonraki adımlar tamamlanamadı.";

        }

        if (lower.contains("your shopping cart is empty") || lower.contains("boş sepet")) {

            return "Sepet boş olmasına rağmen işlem devam etti veya beklenen uyarı görülmedi.";

        }

        if (lower.contains("timeout") || lower.contains("timed out")) {

            return "Sayfa veya öğe beklenen sürede yüklenmedi; test tamamlanamadı.";

        }

        if (lower.contains("assertionerror") || lower.startsWith("expected")) {

            int colon = msg.indexOf(':');

            if (colon > 0 && colon < msg.length() - 1) {

                msg = msg.substring(colon + 1).trim();

            }

        }

        msg = msg.replaceAll("(?i)for case [A-Z0-9_-]+", "");

        msg = msg.replaceAll("(?i)msg=", "Ekranda görünen mesaj: ");

        msg = msg.replaceAll("url=https?://\\S+", "(ilgili sayfa)");

        msg = msg.replaceAll("expected \\[true\\] but found \\[false\\]", "beklenen sonuç oluşmadı");

        msg = msg.replaceAll("\\s+", " ").trim();

        if (msg.length() > 320) {

            msg = msg.substring(0, 317) + "...";

        }

        return msg.isBlank() ? "Test beklenen sonucu vermedi." : msg;

    }



    private static String extractAfter(String text, String marker) {

        int idx = text.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));

        if (idx < 0) {

            return "";

        }

        return text.substring(idx + marker.length()).trim();

    }



    private static void assignSeverityAndPriority(JiraBugReport report, DashboardTestEntry entry) {

        String msg = safe(entry.errorMessage, "").toLowerCase(Locale.ROOT);

        String std = safe(entry.standard, "").toUpperCase(Locale.ROOT);

        String technique = safe(entry.technique, "").toLowerCase(Locale.ROOT);

        String priority = safe(entry.priority, "").toUpperCase(Locale.ROOT);

        String ai = safe(entry.aiAnalysis, "").toLowerCase(Locale.ROOT);



        report.severity = "Minor";

        report.priority = "Medium";

        report.frequency = "Always";



        if (std.contains("OWASP")

                || technique.contains("security")

                || technique.contains("error-guessing")

                || msg.contains("auth bypass")

                || msg.contains("confirmation link")

                || msg.contains("admin@")

                || msg.contains("güvenlik")) {

            report.severity = "Critical";

            report.priority = "Highest";

        } else if ("P0".equals(priority)

                || msg.contains("checkout")

                || msg.contains("payment")

                || msg.contains("ödeme")

                || ai.contains("yüksek")) {

            report.severity = "Major";

            report.priority = "High";

        } else if ("P1".equals(priority) || "P2".equals(priority)) {

            report.severity = "Major";

            report.priority = "Medium";

        }



        if (ai.contains("flaky") && !"Critical".equals(report.severity)) {

            report.frequency = "Intermittent";

        }

    }



    private static String moduleFriendly(String module) {

        return switch (module) {

            case "Checkout" -> "Ödeme";

            case "Cart" -> "Sepet";

            case "Login" -> "Giriş";

            case "Register" -> "Kayıt";

            case "ForgotPassword" -> "Şifremi Unuttum";

            case "Search" -> "Arama";

            case "WishList" -> "İstek Listesi";

            case "MyAccount" -> "Hesabım";

            case "MyAccountSettings" -> "Hesap Ayarları";

            case "Newsletter" -> "Bülten";

            case "Api" -> "API Servisleri";

            case "Db" -> "Veritabanı";

            default -> module;

        };

    }



    private static String techniqueFriendly(String technique) {

        if (technique == null || technique.isBlank()) {

            return "—";

        }

        return switch (technique.toLowerCase(Locale.ROOT)) {

            case "negative-test" -> "Olumsuz Test";

            case "positive-test" -> "Olumlu Test";

            case "boundary-value" -> "Sınır Değer";

            case "equivalence-partitioning" -> "Eşdeğer Sınıflar";

            case "error-guessing" -> "Hata Tahmini";

            case "security" -> "Güvenlik Testi";

            case "state-transition" -> "Durum Geçişi";

            default -> technique;

        };

    }



    private static boolean isTurkish(String text) {

        if (text == null || text.isBlank()) {

            return false;

        }

        return text.matches(".*[ğüşıöçĞÜŞİÖÇ].*");

    }



    private static boolean isApiLayer(DashboardTestEntry entry) {

        return safe(entry.caseId, "").toUpperCase(Locale.ROOT).startsWith("API-");

    }



    private static boolean isDbLayer(DashboardTestEntry entry) {

        return safe(entry.caseId, "").toUpperCase(Locale.ROOT).startsWith("DB-");

    }



    private static String buildLogFileName(DashboardTestEntry entry) {

        return "hata-log-" + sanitize(entry.caseId) + ".txt";

    }



    private static String sanitize(String value) {

        return safe(value, "FAIL").replaceAll("[^A-Za-z0-9_-]", "_");

    }



    private static String shortenPlain(String text, int max) {

        if (text.length() <= max) {

            return text;

        }

        return text.substring(0, max - 1).trim() + "…";

    }



    private static String safe(String value, String fallback) {

        return value == null || value.isBlank() ? fallback : value.trim();

    }

}


