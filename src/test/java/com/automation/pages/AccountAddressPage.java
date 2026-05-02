package com.automation.pages;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigManager;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AccountAddressPage {
    private final WebDriver driver;

    public AccountAddressPage() {
        this.driver = DriverFactory.getDriver();
    }

    public void openList() {
        driver.get(AccountUrlHelper.route("account/address"));
    }

    public void openAddForm() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("content")));
        WebElement add = null;
        try {
            add = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("New Address")));
        } catch (Exception e) {
            try {
                add = driver.findElement(By.partialLinkText("Add Address"));
            } catch (Exception e2) {
                driver.get(AccountUrlHelper.route("account/address/add"));
                return;
            }
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", add);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input-firstname")));
    }

    public void fillAndSaveAddress(String firstName,
                                   String lastName,
                                   String line1,
                                   String city,
                                   String postcode) {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sec));
        WebElement form = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#content form")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("input-firstname")));

        WebElement firstNameEl = visibleField(form, By.id("input-firstname"));
        firstNameEl.clear();
        firstNameEl.sendKeys(firstName == null ? "" : firstName);

        WebElement lastNameEl = visibleField(form, By.id("input-lastname"));
        lastNameEl.clear();
        lastNameEl.sendKeys(lastName == null ? "" : lastName);

        WebElement line1El = visibleField(form, By.id("input-address-1"));
        line1El.clear();
        line1El.sendKeys(line1 == null ? "" : line1);

        WebElement cityEl = visibleField(form, By.id("input-city"));
        cityEl.clear();
        cityEl.sendKeys(city == null ? "" : city);
        try {
            WebElement pc = visibleField(form, By.id("input-postcode"));
            pc.clear();
            pc.sendKeys(postcode == null ? "" : postcode);
        } catch (Exception ignored) {
            // optional on some locales
        }

        WebElement countryEl = visibleFieldAny(form, List.of(
                By.id("input-country"),
                By.name("country_id"),
                By.xpath(".//label[contains(normalize-space(.),'Country')]/following::select[1]"),
                By.xpath(".//select[contains(@id,'country') or contains(@name,'country')]")
        ));
        // JS-based selection avoids stale issues in dynamic OpenCart dropdowns.
        ((JavascriptExecutor) driver).executeScript(
                "var s=arguments[0];"
                        + "for (var i=0;i<s.options.length;i++){"
                        + " var t=(s.options[i].text||'').toLowerCase();"
                        + " if(t.includes('turkey')||t.includes('united states')){ s.selectedIndex=i; break; }"
                        + "}"
                        + "if(!s.value || s.value==='0'){"
                        + " for (var j=0;j<s.options.length;j++){ var v=s.options[j].value;"
                        + "   if(v && v!=='0'){ s.selectedIndex=j; break; }"
                        + " }"
                        + "}"
                        + "s.dispatchEvent(new Event('change',{bubbles:true}));",
                countryEl);

        WebDriverWait zoneWait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(sec, 15)));
        zoneWait.until(d -> {
            WebElement zel = visibleFieldAny(form, List.of(
                    By.id("input-zone"),
                    By.name("zone_id"),
                    By.xpath(".//label[contains(normalize-space(.),'Region') or contains(normalize-space(.),'State')]/following::select[1]"),
                    By.xpath(".//select[contains(@id,'zone') or contains(@name,'zone')]")
            ));
            if (!"select".equalsIgnoreCase(zel.getTagName())) {
                return false;
            }
            Select z = new Select(zel);
            return z.getOptions().size() > 1;
        });
        WebElement zoneEl = visibleFieldAny(form, List.of(
                By.id("input-zone"),
                By.name("zone_id"),
                By.xpath(".//label[contains(normalize-space(.),'Region') or contains(normalize-space(.),'State')]/following::select[1]"),
                By.xpath(".//select[contains(@id,'zone') or contains(@name,'zone')]")
        ));
        ((JavascriptExecutor) driver).executeScript(
                "var s=arguments[0];"
                        + "for (var i=0;i<s.options.length;i++){ var v=s.options[i].value;"
                        + " if(v && v!=='0'){ s.selectedIndex=i; break; }}"
                        + "s.dispatchEvent(new Event('change',{bubbles:true}));",
                zoneEl);

        WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("form[action*='address'] input.btn-primary[type='submit'], "
                        + "form[action*='address'] button[type='submit'], "
                        + "#content form input.btn-primary[type='submit'], "
                        + "#content form button[type='submit']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submit);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit);
    }

    public boolean hasSuccessIndication() {
        int sec = ConfigManager.getInt("explicit.wait.seconds", 8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(sec, 12)));
        try {
            return wait.until(d -> {
                for (WebElement el : d.findElements(By.cssSelector(".alert-danger, .alert.alert-danger"))) {
                    if (el.isDisplayed() && !el.getText().isBlank()) {
                        throw new IllegalStateException("Address validation error: " + el.getText().trim());
                    }
                }
                String u = d.getCurrentUrl();
                if (u.contains("route=account/address") && !u.contains("/add")) {
                    return true;
                }
                for (WebElement el : d.findElements(By.cssSelector(".alert-success, .alert.alert-success"))) {
                    if (el.isDisplayed() && !el.getText().isBlank()) {
                        return true;
                    }
                }
                return false;
            });
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    private static WebElement visibleField(WebElement form, By by) {
        List<WebElement> fields = form.findElements(by);
        for (WebElement el : fields) {
            try {
                if (el.isDisplayed()) {
                    return el;
                }
            } catch (Exception ignored) {
                // stale/hidden, try next
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Visible field not found: " + by);
    }

    private static WebElement visibleFieldAny(WebElement form, List<By> candidates) {
        for (By by : candidates) {
            try {
                return visibleField(form, by);
            } catch (Exception ignored) {
                // try next locator
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Visible field not found with candidate list");
    }
}
