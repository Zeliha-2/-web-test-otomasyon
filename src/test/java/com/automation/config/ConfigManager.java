package com.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public final class ConfigManager {
    private static final Properties PROPERTIES = new Properties();

    static {
        loadClasspathProperties("config/config.properties", true);
        loadClasspathProperties("config/config.local.properties", false);
        normalizeKeys();
    }

    private ConfigManager() {
    }

    public static String get(String key) {
        String resolved = resolveSecret(key);
        if (resolved != null) {
            return resolved;
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

    /**
     * Öncelik: alias env → dotted env → system property → config dosyaları.
     */
    private static String resolveSecret(String key) {
        String alias = aliasEnv(key);
        if (alias != null && !alias.isBlank()) {
            return alias.trim();
        }
        String envDotted = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envDotted != null && !envDotted.isBlank()) {
            return envDotted.trim();
        }
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        return null;
    }

    private static String aliasEnv(String key) {
        return switch (key) {
            case "api.reqres.api.key" -> firstNonBlank(
                    System.getenv("API_REQRES_API_KEY"),
                    System.getenv("REQRES_API_KEY"));
            case "ai.api.key" -> firstNonBlank(
                    System.getenv("AI_API_KEY"),
                    System.getenv("GROQ_API_KEY"));
            case "db.sqlserver.password" -> firstNonBlank(
                    System.getenv("DB_SQLSERVER_PASSWORD"),
                    System.getenv("DB_PASSWORD"));
            case "db.sqlserver.user" -> firstNonBlank(
                    System.getenv("DB_SQLSERVER_USER"),
                    System.getenv("DB_USER"));
            case "test.login.email" -> System.getenv("TEST_LOGIN_EMAIL");
            case "test.login.password" -> System.getenv("TEST_LOGIN_PASSWORD");
            case "jira.api.token" -> firstNonBlank(
                    System.getenv("JIRA_API_TOKEN"),
                    System.getenv("ATLASSIAN_API_TOKEN"));
            case "jira.email" -> System.getenv("JIRA_EMAIL");
            case "jira.base.url" -> System.getenv("JIRA_BASE_URL");
            default -> null;
        };
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void loadClasspathProperties(String resourcePath, boolean required) {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                if (required) {
                    throw new IllegalStateException(resourcePath + " not found under src/test/resources");
                }
                return;
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + resourcePath, e);
        }
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
