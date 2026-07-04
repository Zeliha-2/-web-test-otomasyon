package com.automation.utils;

import com.automation.dashboard.SelfHealingTelemetry;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class SelfHealingElementFinder {
    private SelfHealingElementFinder() {
    }

    public static WebElement find(WebDriver driver, List<By> locatorPriority) {
        for (int i = 0; i < locatorPriority.size(); i++) {
            By locator = locatorPriority.get(i);
            try {
                WebElement element = driver.findElement(locator);
                recordFallback(i, locator);
                return element;
            } catch (NoSuchElementException ignored) {
                // Try next locator candidate.
            }
        }
        throw new NoSuchElementException("Element not found with any fallback locator.");
    }

    public static WebElement findWithin(WebElement root, List<By> locatorPriority) {
        for (int i = 0; i < locatorPriority.size(); i++) {
            By locator = locatorPriority.get(i);
            try {
                WebElement element = root.findElement(locator);
                recordFallback(i, locator);
                return element;
            } catch (NoSuchElementException ignored) {
                // Try next locator candidate.
            }
        }
        throw new NoSuchElementException("Element not found within container with any fallback locator.");
    }

    public static boolean anyPresent(WebDriver driver, List<By> locatorPriority) {
        for (By locator : locatorPriority) {
            if (!driver.findElements(locator).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void recordFallback(int index, By locator) {
        if (index > 0) {
            System.out.println("[SELF-HEAL] Fallback locator used: " + locator);
            SelfHealingTelemetry.recordFallbackLocator(locator.toString());
        }
    }
}
