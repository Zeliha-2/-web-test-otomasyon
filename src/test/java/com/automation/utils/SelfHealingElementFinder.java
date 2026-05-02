package com.automation.utils;

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
                if (i > 0) {
                    System.out.println("[SELF-HEAL] Fallback locator used: " + locator);
                }
                return element;
            } catch (NoSuchElementException ignored) {
                // Try next locator candidate.
            }
        }
        throw new NoSuchElementException("Element not found with any fallback locator.");
    }
}
