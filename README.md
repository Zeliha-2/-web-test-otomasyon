# AI Destekli 3 Katmanli Test Otomasyon Frameworku

Bu repo, VS Code ile baslayabilecegin Java + Maven tabanli bir iskelet framework sunar:
- UI testi: Selenium 4 + Page Object + Self-Healing fallback locator
- API testi: RestAssured
- DB dogrulamasi: JDBC (MS SQL Server)
- Raporlama: TestNG + ExtentReports
- AI root cause: Placeholder entegrasyon noktasi (Claude/Gemini)

## 1) Gereksinimler
- JDK 17+
- Maven 3.9+
- Google Chrome
- VS Code + "Extension Pack for Java"

Kontrol:
```bash
java -version
mvn -version
```

## 2) Projeyi Ac
```bash
cd "web test otomasyon"
code .
```

## 3) Konfigrasyon
`src/test/resources/config/config.properties` dosyasini kendi ortamina gore guncelle:
- `base.url`
- `api.base.url`
- `db.url`, `db.user`, `db.password`
- `ai.api.key`

Not: `ConfigManager`, ortam degiskeni ile override destekler.
Ornek: `AI_API_KEY`, `BASE_URL`, `DB_URL` vb.

## 4) Test Calistirma
Varsayilan (UI regression):
```bash
mvn clean test
```

UI smoke:
```bash
mvn -Dsurefire.suiteXmlFiles=ui-smoke.xml test
```

UI regression:
```bash
mvn -Dsurefire.suiteXmlFiles=ui-regression.xml test
```

Tek test sinifi:
```bash
mvn -Dtest=LoginSmokeTest test
```

Rapor:
- `target/reports/extent-report.html`
- Hata aninda screenshot: `target/screenshots/`

## 5) Framework Yapisi
```text
src/test/java/com/automation
  base/        -> Driver/BaseTest
  config/      -> Config manager
  pages/       -> Page Objects
  utils/       -> API client, self-healing, AI analyzer
  db/          -> JDBC dogrulama
  listeners/   -> TestNG listener + screenshot + report
  tests/       -> Test senaryolari
```

## 6) UI Data-Driven Notlari
1. Login senaryolari: `src/test/resources/testdata/login-scenarios.json`
2. Data-driven test: `com.automation.tests.LoginUiTest`
3. Smoke test: `com.automation.tests.LoginSmokeTest`
4. Sonraki featureler icin ayni modelle yeni JSON + test sinifi eklenir.
