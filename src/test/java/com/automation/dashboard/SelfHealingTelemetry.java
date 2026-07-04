package com.automation.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-thread self-healing events so parallel tests do not mix telemetry.
 */
public final class SelfHealingTelemetry {
    private static final ThreadLocal<List<String>> FALLBACK_LOCATORS =
            ThreadLocal.withInitial(ArrayList::new);

    private SelfHealingTelemetry() {
    }

    public static void recordFallbackLocator(String locatorDescription) {
        if (locatorDescription != null && !locatorDescription.isBlank()) {
            FALLBACK_LOCATORS.get().add(locatorDescription.trim());
        }
    }

    /** Returns a copy of fallback locators used in this test thread and clears the buffer. */
    public static List<String> drainFallbackLocators() {
        List<String> list = FALLBACK_LOCATORS.get();
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(list);
        list.clear();
        return copy;
    }
}
