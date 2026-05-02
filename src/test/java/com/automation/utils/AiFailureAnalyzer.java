package com.automation.utils;

import com.automation.config.ConfigManager;
import java.time.LocalDateTime;

public final class AiFailureAnalyzer {
    private AiFailureAnalyzer() {
    }

    public static String analyze(String errorSummary) {
        String provider = ConfigManager.get("ai.provider");
        String key = ConfigManager.get("ai.api.key");
        if (key == null || key.isBlank() || key.contains("PUT_YOUR")) {
            return "[AI-SKIPPED] API key not configured.";
        }

        // Placeholder hook: add real Claude/Gemini HTTP call here.
        return "[AI-" + provider + "] " + LocalDateTime.now() +
                " probable root cause: UI locator or backend dependency issue. Detail: " + errorSummary;
    }
}
