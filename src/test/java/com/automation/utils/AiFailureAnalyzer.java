package com.automation.utils;

import com.automation.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class AiFailureAnalyzer {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_ML_ENDPOINT = "http://127.0.0.1:5000/predict";
    private static final HttpClient LOCAL_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private AiFailureAnalyzer() {
    }

    public static String analyze(
            String errorSummary,
            String module,
            String technique,
            String standard,
            String testType,
            String testLevel,
            boolean selfHealUsed,
            String duration) {
        String mlPrefix = callMlPredict(module, technique, standard, testType, testLevel, selfHealUsed, duration);
        String groqPart = callGroqAnalysis(errorSummary);
        if (mlPrefix.isBlank()) {
            return groqPart;
        }
        if (groqPart.isBlank()) {
            return mlPrefix;
        }
        return mlPrefix + "\n" + groqPart;
    }

    private static String callMlPredict(
            String module,
            String technique,
            String standard,
            String testType,
            String testLevel,
            boolean selfHealUsed,
            String duration) {
        try {
            String endpoint = ConfigManager.get("ai.ml.endpoint");
            if (endpoint == null || endpoint.isBlank()) {
                endpoint = DEFAULT_ML_ENDPOINT;
            }

            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("module", safe(module));
            payload.put("technique", safe(technique));
            payload.put("standard", safe(standard));
            payload.put("testType", safe(testType));
            payload.put("testLevel", safe(testLevel));
            payload.put("selfHealUsed", selfHealUsed);
            payload.put("duration", safe(duration));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = LOCAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "[ML-MODEL] Servis hatasi: HTTP " + response.statusCode();
            }

            JsonNode root = MAPPER.readTree(response.body());
            String prediction = root.path("prediction").asText("unknown").trim().toLowerCase();
            double probability = root.path("probability").asDouble(0.0);
            String risk = mapRisk(prediction, probability);

            return "[ML-MODEL] Tahmin: " + prediction + " | Risk: " + risk
                    + " | Olasilik: " + String.format(java.util.Locale.ROOT, "%.2f", probability);
        } catch (Exception e) {
            return "[ML-MODEL] Kullanilamiyor: " + e.getMessage();
        }
    }

    private static String mapRisk(String prediction, double probability) {
        if ("flaky".equals(prediction)) {
            if (probability >= 0.75) {
                return "Yüksek";
            }
            if (probability >= 0.5) {
                return "Orta";
            }
            return "Düşük";
        }
        if (probability >= 0.75) {
            return "Düşük";
        }
        return "Orta";
    }

    private static String callGroqAnalysis(String errorSummary) {
        String key = ConfigManager.get("ai.api.key");
        String endpoint = ConfigManager.get("ai.endpoint");
        String model = ConfigManager.get("ai.model");

        if (key == null || key.isBlank() || key.contains("PUT_YOUR")) {
            return "[AI-GROQ] API key not configured.";
        }

        try {
            String prompt = "Sen QATest-AI adli, web test otomasyonu icin fine-tune edilmis bir QA analiz modelisin. "
                + "Asagidaki Selenium/TestNG test hatasini incele ve YALNIZCA su Markdown formatinda Turkce yanit ver:\n\n"
                + "## Kok Neden\n"
                + "(Teknik kok neden — 2-3 cumle, hata mesajindaki URL/assertion detaylarini kullan)\n\n"
                + "## Onerilen Aksiyon\n"
                + "- (Madde 1)\n"
                + "- (Madde 2)\n\n"
                + "## Risk Degerlendirmesi\n"
                + "Dusuk, Orta veya Yuksek — kisa gerekce\n\n"
                + "Test baglami:\n" + errorSummary;

            String body = buildGroqRequestBody(model, prompt);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = createTrustAllHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            JsonNode root = MAPPER.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isEmpty()) {
                return "[AI-GROQ] Model returned empty analysis.";
            }

            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return "[AI-GROQ|" + stamp + "]\n" + content;

        } catch (Exception e) {
            return "[AI-GROQ] " + e.getMessage();
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String buildGroqRequestBody(String model, String prompt) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        root.put("max_tokens", 500);
        return MAPPER.writeValueAsString(root);
    }

    private static HttpClient createTrustAllHttpClient() throws Exception {
        TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new SecureRandom());
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("");

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .sslParameters(sslParameters)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
