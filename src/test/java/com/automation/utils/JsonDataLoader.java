package com.automation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public final class JsonDataLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonDataLoader() {
    }

    public static <T> T load(String resourcePath, Class<T> clazz) {
        try (InputStream input = JsonDataLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            return OBJECT_MAPPER.readValue(input, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read json: " + resourcePath, e);
        }
    }
}
