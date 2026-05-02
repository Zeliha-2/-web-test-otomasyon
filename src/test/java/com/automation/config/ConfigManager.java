package com.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public final class ConfigManager {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config/config.properties")) {
            if (input == null) {
                throw new IllegalStateException("config.properties not found under src/test/resources/config");
            }
            PROPERTIES.load(input);
            normalizeKeys();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigManager() {
    }

    public static String get(String key) {
        String envOverride = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envOverride != null && !envOverride.isBlank()) {
            return envOverride;
        }
        String value = PROPERTIES.getProperty(key);
        return value == null ? null : value.trim();
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static void normalizeKeys() {
        // PowerShell Set-Content may write UTF-8 BOM; remove BOM from first key if present.
        Set<String> keys = PROPERTIES.stringPropertyNames();
        for (String originalKey : keys.toArray(new String[0])) {
            String normalizedKey = originalKey.replace("\uFEFF", "").trim();
            if (!normalizedKey.equals(originalKey)) {
                String value = PROPERTIES.getProperty(originalKey);
                PROPERTIES.remove(originalKey);
                PROPERTIES.setProperty(normalizedKey, value == null ? "" : value.trim());
            } else {
                String value = PROPERTIES.getProperty(originalKey);
                if (value != null) {
                    PROPERTIES.setProperty(originalKey, value.trim());
                }
            }
        }
    }
}
