# AI Destekli 3 Katmanlı Test Otomasyon Framework'ü

LambdaTest E-Commerce Playground üzerinde çalışan, **UI · API · DB · E2E** katmanlarını tek çatı altında toplayan Java + Maven tabanlı test otomasyon projesi. Self-healing locator, Groq/ML destekli hata analizi ve **QA Command Center** dashboard ile birlikte gelir.

## Öne Çıkan Özellikler

| Alan | Açıklama |
|------|----------|
| **UI katmanı** | Selenium 4, Page Object Model, data-driven JSON senaryoları, 10 modül |
| **API katmanı** | RestAssured ile reqres.in senaryoları |
| **DB katmanı** | H2 (in-memory) + SQL Server JDBC doğrulama |
| **E2E** | UI checkout → API sync → SQL Server çapraz katman akışı |
| **Self-healing** | Birincil locator başarısız olunca yedek locator zinciri |
| **AI analiz** | Groq LLM + opsiyonel Flask ML modeli (`ai-analysis/`) |
| **Dashboard** | Türkçe QA Command Center — geçmiş, trend, OWASP, Jira formu |
| **Raporlama** | ExtentReports, teknik hata logları, otomatik ekran görüntüsü |

## Test Kapsamı (özet)

| Katman | Modül / Alan | Senaryo sayısı |
|--------|----------------|----------------|
| UI | Login, Register, ForgotPassword, MyAccount, MyAccountSettings, Newsletter, WishList, Search, Cart, Checkout | ~202 |
| API | REST uç noktaları (reqres.in) | 32 |
| DB | H2 + SQL Server | 35 |
| E2E | Sipariş akışı | 4 |
| **Toplam** | | **~273** |

> Senaryo hedefleri modül riskine göre ayarlanmıştır (Checkout 28, Login/ForgotPassword 24, Newsletter 10 vb.). Güncelleme: `node scripts/rebalance-scenario-targets.js`

## Gereksinimler

- **JDK 17+**
- **Maven 3.9+**
- **Google Chrome** (WebDriverManager sürücüyü indirir)
- **Node.js** (opsiyonel — dashboard serve, senaryo scriptleri)
- **Python 3.10+** (opsiyonel — ML/Flask servisi)
- **SQL Server** (opsiyonel — `SqlServerDbTest` ve E2E için)

Kontrol:

```bash
java -version
mvn -version
```

## Hızlı Başlangıç

```bash
cd "web test otomasyon"
code .
```

### 1) Konfigürasyon

`src/test/resources/config/config.example.properties` dosyasını referans alarak `config.properties` oluştur veya düzenle:

| Anahtar | Açıklama |
|---------|----------|
| `base.url` | UI test mağaza URL'si |
| `api.base.url` | API taban URL (reqres.in) |
| `ai.api.key` | Groq API anahtarı |
| `db.sqlserver.*` | SQL Server bağlantı bilgileri |

Ortam değişkeni override desteklenir: `AI_API_KEY`, `BASE_URL`, `DB_SQLSERVER_PASSWORD`, `API_REQRES_API_KEY` vb.

### 2) Tam regresyon koşumu

```bash
mvn clean test
```

Varsayılan suite: `ui-regression.xml` (pom.xml içinde tanımlı).

### 3) Modül modül koşum (dashboard geçmişi biriktirmek için)

İlk koşumdan sonra **`clean` kullanma** — `target/test-history` silinir.

```bash
mvn test "-Dtest=LoginUiTest"
mvn test "-Dtest=RegisterUiTest"
mvn test "-Dtest=ForgotPasswordUiTest"
mvn test "-Dtest=MyAccountUiTest"
mvn test "-Dtest=MyAccountSettingsUiTest"
mvn test "-Dtest=NewsletterUiTest"
mvn test "-Dtest=WishListUiTest"
mvn test "-Dtest=SearchUiTest"
mvn test "-Dtest=CartUiTest"
mvn test "-Dtest=CheckoutUiTest"
mvn test "-Dtest=ApiTest"
mvn test "-Dtest=DbTest"
mvn test "-Dtest=SqlServerDbTest"
mvn test "-Dtest=E2EOrderFlowTest"
```

### 4) QA Command Center dashboard

```powershell
Copy-Item "src\test\resources\test-history\dashboard.html" "target\test-history\dashboard.html" -Force
cd target\test-history
npx --yes serve . -l 3000
```

Tarayıcı: [http://localhost:3000/dashboard.html](http://localhost:3000/dashboard.html) — hard refresh: `Ctrl+Shift+R`

Dashboard `results.json`, `screenshots/`, `logs/` dosyalarını okur. Test koşumları `DashboardResultsListener` ile otomatik yazılır.

## Diğer Koşum Komutları

```bash
# Smoke
mvn -Dsurefire.suiteXmlFiles=ui-smoke.xml test

# Regression (açık suite)
mvn -Dsurefire.suiteXmlFiles=ui-regression.xml test

# Tek sınıf
mvn -Dtest=LoginUiTest test

# Tek senaryo grubu (TestNG groups varsa)
mvn -Dtest=CheckoutUiTest test
```

## Raporlar ve Çıktılar

| Çıktı | Konum |
|-------|--------|
| Extent HTML raporu | `target/reports/extent-report.html` |
| Dashboard verisi | `target/test-history/results.json` |
| Ekran görüntüleri | `target/test-history/screenshots/` |
| Teknik hata logları | `target/test-history/logs/` |
| Surefire XML/HTML | `target/surefire-reports/` |

## Proje Yapısı

```text
src/test/java/com/automation/
  base/           DriverFactory, BaseTest
  config/         ConfigManager (env override)
  pages/          Page Object sınıfları
  models/         JSON senaryo modelleri
  tests/          TestNG test sınıfları
  utils/          SelfHealingElementFinder, AiFailureAnalyzer, ApiClient
  db/             DbClient, DbInitializer
  dashboard/      DashboardResultsListener, JiraBugReportBuilder, FailureLogWriter
  listeners/      TestListener (Extent + screenshot)

src/test/resources/
  config/         config.properties, config.example.properties
  testdata/       *-scenarios.json (data-driven)
  test-history/   dashboard.html (kaynak şablon)
  sql/            setup_testdb.sql, check_order.sql

ai-analysis/      Flask ML servisi (opsiyonel)
scripts/          Senaryo dengeleme ve yardımcı scriptler
docs/latex/       Bitirme / vize raporu LaTeX kaynakları
```

## AI ve ML Servisi (opsiyonel)

```bash
cd ai-analysis
pip install -r requirements.txt
python app.py
```

Flask servisi `http://127.0.0.1:5000` üzerinde flaky/stabil tahmini yapar. Groq analizi `config.properties` içindeki `ai.api.key` ile doğrudan Java tarafından da çalışır.

## Jira Hata Raporlama

- Dashboard içinde **Hata Analizi → Hatayı Raporla** ile Türkçe Jira formatında rapor üretilir.
- Kayıtlar tarayıcı `localStorage` ve isteğe bağlı `jira-reports.json` ile saklanır.
- Proje anahtarı varsayılan: `LTPLAY`
- REST API ile otomatik issue açma: `jira.enabled=true` + token (geliştirme aşamasında)

## Bilinçli Güvenlik Senaryoları

ForgotPassword modülünde OWASP odaklı senaryolar (ör. `FPW-EG-01`, `FPW-ST-02`) **bilerek fail** olacak şekilde tasarlanmıştır — gerçek uygulama güvenlik açıklarını simüle eder.

## LaTeX Rapor

Bitirme / vize raporu için hazır iskelet:

```bash
docs/latex/rapor.tex          # Ana dosya
docs/latex/icindekiler.tex    # Manuel içindekiler (tahmini sayfa numaraları)
docs/latex/bolumler/          # Bölüm dosyaları
```

Derleme (TeX Live / MiKTeX):

```bash
cd docs/latex
pdflatex rapor.tex
pdflatex rapor.tex
```

## Gelecek İyileştirmeler

- GitHub Actions CI/CD pipeline
- Jira REST API ile otomatik issue oluşturma
- Dashboard tek komutla serve (`npm run dashboard`)
- Paralel koşum rapor birleştirme iyileştirmeleri

## Lisans

Eğitim / portfolyo amaçlı açık kaynak iskelet. LambdaTest Playground ve reqres.in kendi kullanım koşullarına tabidir.
