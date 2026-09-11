package com.selenium.test;

import com.selenium.utils.SlackService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SMS Share Form Automation Test
 * 
 * This test automates the SMS Share form functionality:
 * 1. Opens the website
 * 2. Clicks Share icon
 * 3. Selects SMS tab
 * 4. Selects country code (India)
 * 5. Enters mobile number
 * 6. Accepts terms and conditions
 * 7. Clicks Share button
 */
public class SMSShareTest {
    
    // WebDriver instance
    private WebDriver driver;
    private WebDriverWait wait;
    
    // Slack Service (optional - set via system properties)
    private static SlackService slackService;
    
    // Slack Configuration (system properties -Dslack.webhook.url=... or env SLACK_WEBHOOK_URL)
    // Example: -Dslack.webhook.url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL -Dslack.channel=#test-results -Dslack.username=TestBot
    private static final String SLACK_WEBHOOK_URL = getSlackConfig("slack.webhook.url", "SLACK_WEBHOOK_URL", "");
    private static final String SLACK_CHANNEL = getSlackConfig("slack.channel", "SLACK_CHANNEL", "");
    private static final String SLACK_USERNAME = getSlackConfig("slack.username", "SLACK_USERNAME", "SMS Share Test Bot");

    /** Prefer system property, fallback to environment variable. */
    private static String getSlackConfig(String sysProp, String envVar, String defaultValue) {
        String value = System.getProperty(sysProp);
        if (value != null && !value.isEmpty()) return value;
        value = System.getenv(envVar);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
    
    // Headless Mode Configuration (can be set via system properties)
    // Example: -Dheadless=true (runs browser in background without opening window)
    private static final boolean HEADLESS_MODE = Boolean.parseBoolean(System.getProperty("headless", "true"));
    private static final int PAGE_LOAD_TIMEOUT_SECONDS = 120;
    private static final int PAGE_LOAD_MAX_ATTEMPTS = 2;
    
    // Test data - Multiple URLs to test (including Indiana)
    private static final String[] TEST_URLS = {
        "https://livewell.wicresources.org/",
        "https://westvirginia.wicresources.org/",
        "https://oklahoma.wicresources.org/",
        "https://oregon.wicresources.org/",
        "https://delaware.wicresources.org/",
        "https://indiana.wicresources.org/",
        "https://indiana.wicresources.org/breastfeeding/",
        "https://kansaswic.wicresources.org/approved-food-list/",
        "https://newjersey.wicresources.org/",
        "https://connecticut.wicresources.org/",
        "https://nebraska.wicresources.org/",
        "https://chickasawnation.ebtresources.org/summer-ebt-approved-food-list/"
    };
    private static final String MOBILE_NUMBER = "7428730894";
    // Above is just a test number, not the actual number for the test.
    
    // Test result data structure
    private static class TestResult {
        String url;
        String status;
        String captchaStatus;
        String successMessage;
        String errorMessage;
        long duration;
        
        TestResult(String url) {
            this.url = url;
            this.status = "FAIL";
            this.captchaStatus = "N/A";
            this.successMessage = "N/A";
            this.errorMessage = "N/A";
            this.duration = 0;
        }
    }
    
    // XPath configuration class to hold XPaths for each site
    private static class XPathConfig {
        String preOpenPopup;
        String shareIcon;
        String smsTab;
        String countryCodeDropdown;
        String indiaOption;
        String mobileNumberInput;
        String termsCheckbox;
        String shareButton;
        
        XPathConfig(String shareIcon, String smsTab, String countryCodeDropdown, 
                   String indiaOption, String mobileNumberInput, String termsCheckbox, String shareButton) {
            this(null, shareIcon, smsTab, countryCodeDropdown, indiaOption, mobileNumberInput, termsCheckbox, shareButton);
        }

        XPathConfig(String preOpenPopup, String shareIcon, String smsTab, String countryCodeDropdown, 
                   String indiaOption, String mobileNumberInput, String termsCheckbox, String shareButton) {
            this.preOpenPopup = preOpenPopup;
            this.shareIcon = shareIcon;
            this.smsTab = smsTab;
            this.countryCodeDropdown = countryCodeDropdown;
            this.indiaOption = indiaOption;
            this.mobileNumberInput = mobileNumberInput;
            this.termsCheckbox = termsCheckbox;
            this.shareButton = shareButton;
        }
    }
    
    // Map to store XPath configurations for each URL
    private static final Map<String, XPathConfig> XPATH_MAP = new HashMap<>();
    
    /**
     * Initialize XPath configurations for all URLs
     */
    static {
        // Oklahoma - Specific XPaths
        XPathConfig oklahomaConfig = new XPathConfig(
            "//div[@id='top']/div[2]/div/span",                                                        // Share icon
            "//a[normalize-space()='SMS']",                                                             // SMS tab
            "//div[@id='wpcf7-f25055-o1']/form/div/p/label/span/div/div/div",                           // Country Code dropdown
            "//div[@id='wpcf7-f25055-o1']/form/div/p/label/span/div/div/ul/li[101]/span",               // India option
            "//input[@name='yourphone']",                                                               // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                       // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                                 // Share button
        );
        XPATH_MAP.put("https://oklahoma.wicresources.org/", oklahomaConfig);
        
        // Livewell - Specific XPaths
        XPathConfig livewellConfig = new XPathConfig(
            "(.//*[normalize-space(text()) and normalize-space(.)='Live Well'])[1]/following::span[1]",         // Share icon
            "//a[normalize-space()='SMS']",                                                                     // SMS tab
            "//div[@id='wpcf7-f25072-o1']/form/div/p/label/span/div/div/div",                                  // Country Code dropdown
            "//div[@id='wpcf7-f25072-o1']/form/div/p/label/span/div/div/ul/li[101]/span",                      // India option
            "//input[@name='yourphone']",                                                                      // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                              // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                                        // Share button
        );
        XPATH_MAP.put("https://livewell.wicresources.org/", livewellConfig);
        
        // West Virginia - Specific XPaths
        XPathConfig westVirginiaConfig = new XPathConfig(
            "//div[@id='top']/div[2]/div/span",                                                    // Share icon
            "//a[normalize-space()='SMS']",                                                         // SMS tab
            "//div[@id='wpcf7-f37551-o1']/form/div/p/label/span/div/div/div",                      // Country Code dropdown
            "//div[@id='wpcf7-f37551-o1']/form/div/p/label/span/div/div/ul/li[101]/span",          // India option
            "//input[@name='yourphone']",                                                          // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                  // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                            // Share button
        );
        XPATH_MAP.put("https://westvirginia.wicresources.org/", westVirginiaConfig);
        
        // Oregon - Specific XPaths
        XPathConfig oregonConfig = new XPathConfig(
            "//div[@id='top']/div[2]/div/i",                                                             // Share icon
            "//a[normalize-space()='SMS']",                                                               // SMS tab
            "//div[@id='wpcf7-f62289-o2']/form/div/p/label/span/div/div/div/div[2]",                     // Country Code dropdown
            "//div[@id='wpcf7-f62289-o2']/form/div/p/label/span/div/div/ul/li[101]/span",                // India option
            "//input[@name='yourphone']",                                                                 // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                         // Terms checkbox label
            "//input[@value='Share']"                                                                     // Share button
        );
        XPATH_MAP.put("https://oregon.wicresources.org/", oregonConfig);
        
        // Delaware - Specific XPaths
        XPathConfig delawareConfig = new XPathConfig(
            "//div[contains(@class,'share-btn')]",                                                         // Share icon (class="share-btn")
            "//a[text()='SMS']",                                                                            // SMS tab (link text converted to XPath)
            "//div[@id='wpcf7-f44456-o1']/form/div/p/label/span/div/div/div/div",                          // Country Code dropdown
            "//div[@id='wpcf7-f44456-o1']/form/div/p/label/span/div/div/ul/li[101]/span",                  // India option
            "//input[@name='yourphone']",                                                                   // Mobile Number input (name converted to XPath)
            "//div[@id='wpcf7-f44456-o1']/form/div[2]/p/span/span/span/label/span",                         // Terms checkbox
            "//*[@id='submit-btn-sms']"                                                                      // Share button (id converted to XPath)
        );
        XPATH_MAP.put("https://delaware.wicresources.org/", delawareConfig);
        
        // Indiana - Specific XPaths
        XPathConfig indianaConfig = new XPathConfig(
            "//div[@type='button']",                                                                      // Share icon
            "//a[normalize-space()='SMS']",                                                               // SMS tab
            "//div[@id='wpcf7-f74270-o1']/form/div/p/label/span/div/div/div/div[3]",                      // Country Code dropdown
            "//div[@id='wpcf7-f74270-o1']/form/div/p/label/span/div/div/ul/li[101]/span",                 // India option
            "//*[@id='wliq-phone-1']",                                                                    // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                         // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                                   // Share button
        );
        XPATH_MAP.put("https://indiana.wicresources.org/", indianaConfig);

        // Indiana Breastfeeding page - requires opening content popup first
        XPathConfig indianaBreastfeedingConfig = new XPathConfig(
            "//a[@data-target='#pdf-popup-breastfeeding']",                                                 // Pre-open popup (Infographics modal opener)
            "//button[contains(@class,'infographic-share-btn')]",                                           // Share button in breastfeeding popup
            "//button[@data-tab='sms']",                                                                    // SMS tab in feed popup
            "//div[@id='wpcf7-f82303-o2']/form/div/p/span/div/div/div/div[2]",                             // Country Code dropdown
            "//div[@id='wpcf7-f82303-o2']/form/div/p/span/div/div/ul/li[101]/span",                        // India option
            "//input[@name='yourphoneinfographic']",                                                        // Mobile Number input
            "//div[@id='wpcf7-f82303-o2']/form/div[2]/p/span/span/span/label/span",                         // Terms checkbox
            "//div[@id='wpcf7-f82303-o2']/form/div[3]/div/p/input"                                          // Share button
        );
        XPATH_MAP.put("https://indiana.wicresources.org/breastfeeding/", indianaBreastfeedingConfig);

        // Kansas Approved Food List - Specific XPaths
        XPathConfig kansasApprovedFoodListConfig = new XPathConfig(
            "(.//*[normalize-space(text()) and normalize-space(.)='Download'])[1]/following::span[1]",       // Share icon/button
            "//a[normalize-space()='SMS']",                                                                  // SMS tab
            "//div[@id='wpcf7-f3127-o2']/form/div/p/label/span/div/div/div",                                 // Country Code dropdown
            "//div[@id='wpcf7-f3127-o2']/form/div/p/label/span/div/div/ul/li[101]/span",                     // India option
            "//input[@name='yourphone']",                                                                    // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                            // Terms checkbox label
            "//input[@value='Share']"                                                                        // Share button
        );
        XPATH_MAP.put("https://kansaswic.wicresources.org/approved-food-list/", kansasApprovedFoodListConfig);
        
        // Nebraska - Specific XPaths
        XPathConfig nebraskaConfig = new XPathConfig(
            "//div[@id='top']/div/main/div/div/div/div/div/span",                                      // Share icon
            "//a[normalize-space()='SMS']",                                                             // SMS tab
            "//div[@id='wpcf7-f10351-o1']/form/div/p/label/span/div/div/div",                          // Country Code dropdown
            "//div[@id='wpcf7-f10351-o1']/form/div/p/label/span/div/div/ul/li[101]/span",              // India option
            "//input[@name='yourphone']",                                                               // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                       // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                                 // Share button
        );
        XPATH_MAP.put("https://nebraska.wicresources.org/", nebraskaConfig);
        
        // Chickasaw Nation Summer EBT - Specific XPaths
        XPathConfig chickasawConfig = new XPathConfig(
            "//div[@id='top']/div/div[2]/div[2]/div/div/div[2]/div/div/button/span",                    // Share icon
            "//a[normalize-space()='SMS']",                                                             // SMS tab
            "//div[@id='wpcf7-f156-o1']/form/div/p/label/span/div/div/div",                             // Country Code dropdown
            "//div[@id='wpcf7-f156-o1']/form/div/p/label/span/div/div/ul/li[101]/span",                 // India option
            "//input[@name='yourphone']",                                                               // Mobile Number input
            "//div[@id='wpcf7-f156-o1']/form/div[2]/p/span/span/span/label",                            // Terms checkbox
            "//*[@id='submit-btn-sms']"                                                                 // Share button
        );
        XPATH_MAP.put("https://chickasawnation.ebtresources.org/summer-ebt-approved-food-list/", chickasawConfig);
        
        // New Jersey - Specific XPaths
        XPathConfig newJerseyConfig = new XPathConfig(
            "(.//*[normalize-space(text()) and normalize-space(.)='New Jersey WIC'])[1]/following::span[1]", // Share icon
            "//a[normalize-space()='SMS']",                                                                 // SMS tab
            "//div[@id='wpcf7-f45594-o1']/form/div/p/label/span/div/div/div",                              // Country Code dropdown
            "//div[@id='wpcf7-f45594-o1']/form/div/p/label/span/div/div/ul/li[101]/span",                  // India option
            "//input[@name='yourphone']",                                                                  // Mobile Number input
            "//input[@name='checkbox-403[]']/parent::label/span",                                          // Terms checkbox label
            "//*[@id='submit-btn-sms']"                                                                     // Share button
        );
        XPATH_MAP.put("https://newjersey.wicresources.org/", newJerseyConfig);
        
        // Connecticut - Specific XPaths
        XPathConfig connecticutConfig = new XPathConfig(
            "//div[@id='top']/div[2]/div/button",                                                          // Share icon
            "//a[normalize-space()='SMS']",                                                                // SMS tab
            "//form[@id='sms-form']/div/div/div",                                                          // Country Code dropdown
            "//li[@id='iti-0__item-in']/span",                                                              // India option
            "//*[@id='phone']",                                                                            // Mobile Number input
            "//form[@id='sms-form']/div[2]/label",                                                         // Terms checkbox
            "//*[@id='submit-btn-sms']"                                                                     // Share button
        );
        XPATH_MAP.put("https://connecticut.wicresources.org/", connecticutConfig);
    }
    
    /**
     * Get XPath configuration for a given URL
     */
    private XPathConfig getXPathConfig(String url) {
        XPathConfig config = XPATH_MAP.get(url);
        if (config == null) {
            throw new RuntimeException("No XPath configuration found for URL: " + url);
        }
        return config;
    }
    
    /**
     * Check if CAPTCHA is present on the page
     * Checks for reCAPTCHA/hCaptcha iframes and containers in DOM (even if not visible)
     * @return true if CAPTCHA is found, false otherwise
     */
    private boolean isCaptchaPresent() {
        try {
            // Check for reCAPTCHA/hCaptcha iframes in DOM first
            String[] iframeSelectors = {
                "iframe[src*='recaptcha']",
                "iframe[src*='hcaptcha']",
                "iframe[title*='reCAPTCHA']",
                "iframe[title*='hCaptcha']"
            };
            
            for (String selector : iframeSelectors) {
                try {
                    java.util.List<WebElement> iframes = driver.findElements(By.cssSelector(selector));
                    if (!iframes.isEmpty()) {
                        return true; // Present in DOM (visible or hidden)
                    }
                } catch (Exception e) {
                    // Continue checking
                }
            }
            
            // Check for CAPTCHA containers/divs (must be displayed)
            String[] containerSelectors = {
                "//div[contains(@class, 'g-recaptcha')]",
                "//div[contains(@class, 'recaptcha')]",
                "//div[contains(@id, 'recaptcha')]",
                "//div[contains(@class, 'h-captcha')]",
                "//div[contains(@id, 'captcha')]",
                "//div[contains(@class, 'captcha')]"
            };
            
            for (String selector : containerSelectors) {
                try {
                    WebElement captcha = driver.findElement(By.xpath(selector));
                    if (captcha.isDisplayed()) {
                        return true;
                    }
                } catch (Exception e) {
                    // Continue checking other selectors
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if CAPTCHA is blocking/interfering with form elements
     * @param formElement The form element to check if it's blocked by CAPTCHA
     * @return true if CAPTCHA might be blocking, false otherwise
     */
    private boolean isCaptchaBlocking(WebElement formElement) {
        try {
            if (!isCaptchaPresent()) {
                return false;
            }
            
            // Get form element location
            int formY = formElement.getLocation().getY();
            int formHeight = formElement.getSize().getHeight();
            int formBottom = formY + formHeight;
            
            // Check if any CAPTCHA element is positioned above or overlapping the form
            String[] captchaSelectors = {
                "//iframe[contains(@src, 'recaptcha')]",
                "//div[contains(@class, 'g-recaptcha')]",
                "//div[contains(@class, 'recaptcha')]",
                "//iframe[contains(@src, 'hcaptcha')]",
                "//div[contains(@class, 'h-captcha')]"
            };
            
            for (String selector : captchaSelectors) {
                try {
                    WebElement captcha = driver.findElement(By.xpath(selector));
                    if (captcha.isDisplayed()) {
                        int captchaY = captcha.getLocation().getY();
                        int captchaHeight = captcha.getSize().getHeight();
                        int captchaBottom = captchaY + captchaHeight;
                        
                        // Check if CAPTCHA is positioned below the form (moved down)
                        // This indicates CAPTCHA might be interfering
                        if (captchaY > formY && captchaY < formBottom) {
                            return true; // CAPTCHA is overlapping with form
                        }
                        if (captchaBottom > formY && captchaBottom < formBottom) {
                            return true; // CAPTCHA bottom is overlapping with form
                        }
                        if (captchaY > formBottom - 100) {
                            return true; // CAPTCHA is very close to form, might be blocking
                        }
                    }
                } catch (Exception e) {
                    // Continue checking
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Site-automation flow: Check reCAPTCHA iframe for error (like Playwright site-automation).
     * If reCAPTCHA iframe exists and its body text contains "error", return true (stop flow).
     * If no reCAPTCHA iframe found, return false (continue – page may not use reCAPTCHA).
     * @return true if CAPTCHA has error (should stop flow), false if CAPTCHA OK or not present
     */
    private boolean isRecaptchaFrameHasError() {
        try {
            java.util.List<WebElement> iframes = driver.findElements(By.cssSelector("iframe[src*='recaptcha']"));
            if (iframes.isEmpty()) {
                System.out.println("🟩 CAPTCHA CHECK: No reCAPTCHA iframe (page may not use it) – continuing");
                return false;
            }
            boolean inspectedAnyFrame = false;
            boolean hasError = false;
            for (WebElement iframe : iframes) {
                try {
                    // Prefer visible iframes, but still try hidden ones (some sites keep it in DOM hidden)
                    inspectedAnyFrame = true;
                    driver.switchTo().frame(iframe);
                    String bodyText = driver.findElement(By.tagName("body")).getText();
                    driver.switchTo().defaultContent();
                    if (bodyText != null && bodyText.toLowerCase().contains("error")) {
                        System.out.println("🟥 CAPTCHA CHECK: reCAPTCHA iframe shows error");
                        System.out.println("=========== CAPTCHA ERROR ===========");
                        System.out.println(bodyText.trim());
                        System.out.println("======================================");
                        hasError = true;
                    }
                } catch (Exception e) {
                    try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
                }
            }
            if (hasError) {
                return true;
            }
            if (!inspectedAnyFrame) {
                System.out.println("🟨 CAPTCHA CHECK: reCAPTCHA iframe found but could not be inspected");
                return false;
            }
            System.out.println("🟩 CAPTCHA CHECK: reCAPTCHA widget OK (no visible error)");
            return false;
        } catch (Exception e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            System.out.println("🟩 CAPTCHA CHECK: Check skipped – " + e.getMessage());
            return false;
        }
    }

    private TestResult stopForCaptchaError(TestResult result, long startTime, String stage) {
        System.out.println("\n⛔ STOPPING FLOW — CAPTCHA INVALID (" + stage + ")");
        System.out.println("❌ No SMS flow (no form interaction, no submit)");
        long duration = System.currentTimeMillis() - startTime;
        result.status = "CAPTCHA_ERROR";
        result.captchaStatus = "reCAPTCHA iframe error - flow stopped (" + stage + ")";
        result.errorMessage = "CAPTCHA invalid; automation safely stopped";
        result.duration = duration;
        System.out.println("==========================================");
        System.out.println("TEST RESULT: CAPTCHA_ERROR for " + result.url);
        System.out.println("==========================================\n");
        return result;
    }

    /**
     * Log CAPTCHA-related iframe details for current page.
     * Helps compare iframe differences across sites.
     */
    private void logCaptchaIframes(String stage) {
        try {
            String[] selectors = {
                "iframe[src*='recaptcha']",
                "iframe[src*='hcaptcha']",
                "iframe[title*='reCAPTCHA']",
                "iframe[title*='hCaptcha']"
            };

            System.out.println("CAPTCHA iframe scan (" + stage + "):");
            int total = 0;
            for (String selector : selectors) {
                java.util.List<WebElement> iframes = driver.findElements(By.cssSelector(selector));
                for (WebElement iframe : iframes) {
                    total++;
                    String src = iframe.getAttribute("src");
                    String title = iframe.getAttribute("title");
                    String id = iframe.getAttribute("id");
                    boolean visible = false;
                    try {
                        visible = iframe.isDisplayed();
                    } catch (Exception ignored) {}
                    System.out.println(String.format(
                        "  - #%d visible=%s id=%s title=%s src=%s",
                        total,
                        visible,
                        id != null ? id : "N/A",
                        title != null ? title : "N/A",
                        src != null ? src : "N/A"
                    ));
                }
            }
            if (total == 0) {
                System.out.println("  - none found");
            }
        } catch (Exception e) {
            System.out.println("CAPTCHA iframe scan failed (" + stage + "): " + e.getMessage());
        }
    }

    /**
     * Get CAPTCHA status information
     * @return String describing CAPTCHA status
     */
    private String getCaptchaStatus() {
        try {
            // First check for reCAPTCHA iframes (most common)
            java.util.List<WebElement> recaptchaIframes = driver.findElements(By.cssSelector("iframe[src*='recaptcha']"));
            if (!recaptchaIframes.isEmpty()) {
                int visibleCount = 0;
                for (WebElement iframe : recaptchaIframes) {
                    if (iframe.isDisplayed()) {
                        visibleCount++;
                    }
                }
                if (visibleCount > 0) {
                    WebElement iframe = recaptchaIframes.get(0);
                    int y = iframe.getLocation().getY();
                    int height = iframe.getSize().getHeight();
                    return String.format("CAPTCHA: reCAPTCHA PRESENT ✓ (visible: %d, total iframes: %d) at Y=%d, Height=%d",
                        visibleCount, recaptchaIframes.size(), y, height);
                }
                return String.format("CAPTCHA: reCAPTCHA PRESENT ✓ (in DOM, hidden) total iframes: %d", recaptchaIframes.size());
            }
            
            // Check for hCaptcha iframes
            java.util.List<WebElement> hcaptchaIframes = driver.findElements(By.cssSelector("iframe[src*='hcaptcha']"));
            if (!hcaptchaIframes.isEmpty()) {
                int visibleCount = 0;
                for (WebElement iframe : hcaptchaIframes) {
                    if (iframe.isDisplayed()) {
                        visibleCount++;
                    }
                }
                if (visibleCount > 0) {
                    WebElement iframe = hcaptchaIframes.get(0);
                    int y = iframe.getLocation().getY();
                    int height = iframe.getSize().getHeight();
                    return String.format("CAPTCHA: hCaptcha PRESENT ✓ (visible: %d, total iframes: %d) at Y=%d, Height=%d",
                        visibleCount, hcaptchaIframes.size(), y, height);
                }
                return String.format("CAPTCHA: hCaptcha PRESENT ✓ (in DOM, hidden) total iframes: %d", hcaptchaIframes.size());
            }
            
            // Check for CAPTCHA containers
            String[] containerSelectors = {
                "//div[contains(@class, 'g-recaptcha')]",
                "//div[contains(@class, 'recaptcha')]",
                "//div[contains(@id, 'recaptcha')]",
                "//div[contains(@class, 'h-captcha')]"
            };
            
            for (String selector : containerSelectors) {
                try {
                    WebElement captcha = driver.findElement(By.xpath(selector));
                    if (captcha.isDisplayed()) {
                        int y = captcha.getLocation().getY();
                        int height = captcha.getSize().getHeight();
                        return String.format("CAPTCHA: PRESENT ✓ (container) at Y=%d, Height=%d", y, height);
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            // No CAPTCHA found
            return "CAPTCHA: NOT PRESENT ✗";
        } catch (Exception e) {
            return "CAPTCHA: CHECK FAILED ✗ - " + e.getMessage();
        }
    }
    
    /**
     * Setup method to initialize WebDriver
     */
    public void setUp() {
        // Setup ChromeDriver using WebDriverManager (auto-match installed Chrome)
        WebDriverManager.chromedriver().setup();
        
        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);
        
        // Headless mode: Run browser in background (no visible window)
        if (HEADLESS_MODE) {
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            System.out.println("Running in HEADLESS mode (browser will run in background)");
        } else {
            options.addArguments("--start-maximized");
            System.out.println("Running in VISIBLE mode (browser window will be visible)");
        }
        
        // Common options for both modes
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // Initialize ChromeDriver
        driver = new ChromeDriver(options);
        
        // Some sites intermittently hang on third-party scripts; wait longer and use EAGER load strategy.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS));
        
        // Initialize WebDriverWait with 20 seconds timeout (balanced for reliability and speed)
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        System.out.println("ChromeDriver initialized successfully");
    }
    
    /**
     * Wait for common site preloaders to stop blocking clicks after eager page load.
     */
    private void waitForBlockingLoaders() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".preloader"))
            );
        } catch (Exception e) {
            System.out.println("No blocking preloader detected, continuing...");
        }
    }
    
    /**
     * Test method to execute SMS Share form automation
     * 
     * @param url The URL to test
     * @return TestResult object containing test details
     */
    public TestResult testSMSShareForm(String url) {
        TestResult result = new TestResult(url);
        long startTime = System.currentTimeMillis();
        
        // Get XPath configuration for this URL
        XPathConfig xpaths = getXPathConfig(url);
        
        try {
            // Step 1: Open URL
            System.out.println("\n==========================================");
            System.out.println("Testing URL: " + url);
            System.out.println("==========================================");
            System.out.println("Step 1: Opening URL: " + url);
            org.openqa.selenium.TimeoutException pageLoadTimeout = null;
            boolean pageLoaded = false;
            for (int attempt = 1; attempt <= PAGE_LOAD_MAX_ATTEMPTS; attempt++) {
                try {
                    driver.get(url);
                    System.out.println("Page loaded successfully");
                    pageLoaded = true;
                    break;
                } catch (org.openqa.selenium.TimeoutException e) {
                    pageLoadTimeout = e;
                    System.err.println("Page load timeout for: " + url + " (attempt " + attempt + "/" + PAGE_LOAD_MAX_ATTEMPTS + ")");
                    if (attempt < PAGE_LOAD_MAX_ATTEMPTS) {
                        try {
                            driver.navigate().to("about:blank");
                            Thread.sleep(2000);
                        } catch (Exception resetErr) {
                            System.err.println("Warning: Failed to reset browser before retry: " + resetErr.getMessage());
                        }
                        System.out.println("Retrying page load...");
                    }
                }
            }
            if (!pageLoaded) {
                throw new RuntimeException(
                    "Page load timeout after " + PAGE_LOAD_TIMEOUT_SECONDS + " seconds and " + PAGE_LOAD_MAX_ATTEMPTS + " attempts: " + url,
                    pageLoadTimeout
                );
            }
            Thread.sleep(1000); // Wait for page to load
            waitForBlockingLoaders();
            
            // Check CAPTCHA status after page load
            System.out.println("CAPTCHA Check (Initial): " + getCaptchaStatus());
            logCaptchaIframes("initial");
            
            // Site-automation flow: Check reCAPTCHA first (before SMS flow). If error, stop.
            if (isRecaptchaFrameHasError()) {
                return stopForCaptchaError(result, startTime, "initial");
            }

            // Optional extra step for pages that need opening a popup first
            if (xpaths.preOpenPopup != null && !xpaths.preOpenPopup.isEmpty()) {
                System.out.println("Step 1.5: Opening page-specific popup");
                WebElement preOpenPopupElement = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath(xpaths.preOpenPopup))
                );
                try {
                    preOpenPopupElement.click();
                } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                    // Some pages overlay this CTA; scroll and click via JS as fallback.
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", preOpenPopupElement);
                    Thread.sleep(300);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", preOpenPopupElement);
                }
                Thread.sleep(1000);
            }
            
            // Step 2: Click Share icon (SMS flow)
            System.out.println("Step 2: Clicking Share icon");
            WebElement shareIcon = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.shareIcon)));
            shareIcon.click();
            Thread.sleep(1000); // Wait for share menu to appear
            
            // Step 3: Click SMS tab
            System.out.println("Step 3: Clicking SMS tab");
            WebElement smsTab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.smsTab)));
            smsTab.click();
            Thread.sleep(1000); // Wait for SMS form to load
            
            System.out.println("CAPTCHA Check (After SMS form): " + getCaptchaStatus());
            logCaptchaIframes("after_sms_form");

            // Option A: if CAPTCHA becomes invalid after opening SMS form, stop this URL
            if (isRecaptchaFrameHasError()) {
                return stopForCaptchaError(result, startTime, "after_sms_form");
            }
            
            // Step 4: Click Country Code dropdown
            System.out.println("Step 4: Clicking Country Code dropdown");
            WebElement countryDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.countryCodeDropdown)));
            countryDropdown.click();
            Thread.sleep(500); // Wait for dropdown to open (reduced from 1000ms)
            
            // Step 5: Select India from country list
            System.out.println("Step 5: Selecting India from country list");
            WebElement indiaOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.indiaOption)));
            indiaOption.click();
            Thread.sleep(500); // Wait for selection to be applied (reduced from 1000ms)
            
            // Step 6: Click Mobile Number input
            System.out.println("Step 6: Clicking Mobile Number input");
            WebElement mobileInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.mobileNumberInput)));
            mobileInput.click();
            Thread.sleep(500);
            
            // Step 7: Enter mobile number
            System.out.println("Step 7: Entering mobile number: " + MOBILE_NUMBER);
            mobileInput.clear();
            mobileInput.sendKeys(MOBILE_NUMBER);
            Thread.sleep(1000);
            
            // Step 8: Select Terms checkbox
            System.out.println("Step 8: Selecting Terms checkbox");
            WebElement termsCheckbox = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.termsCheckbox)));
            
            // Click the checkbox (works for both input and span elements)
            // If it's an input checkbox, check if already selected before clicking
            try {
                if (termsCheckbox.getTagName().equalsIgnoreCase("input") 
                    && "checkbox".equalsIgnoreCase(termsCheckbox.getAttribute("type"))) {
                    if (!termsCheckbox.isSelected()) {
                        termsCheckbox.click();
                    }
                } else {
                    // For span or other elements, just click
                    termsCheckbox.click();
                }
            } catch (Exception e) {
                // Fallback: just click if any error occurs
                termsCheckbox.click();
            }
            Thread.sleep(500); // Reduced from 1000ms
            
            // Step 9: Click Share button
            System.out.println("Step 9: Clicking Share button");
            WebElement shareButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.shareButton)));
            
            // Check CAPTCHA status before clicking Share button
            System.out.println("CAPTCHA Check (Before Share click): " + getCaptchaStatus());
            if (isCaptchaBlocking(shareButton)) {
                System.out.println("⚠️  WARNING: CAPTCHA may be blocking the Share button!");
            }

            // Option A: if CAPTCHA becomes invalid before submit, stop this URL
            if (isRecaptchaFrameHasError()) {
                return stopForCaptchaError(result, startTime, "before_submit");
            }
            
            shareButton.click();
            
            // The Indiana breastfeeding popup requires a second submit click.
            if (url.contains("indiana.wicresources.org/breastfeeding/")) {
                System.out.println("Indiana breastfeeding detected: Clicking Share button second time");
                Thread.sleep(500); // Small delay between clicks (reduced from 1000ms)
                shareButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpaths.shareButton)));
                shareButton.click();
            }
            
            // Check CAPTCHA status after clicking Share button
            Thread.sleep(300); // Small delay to let page update (reduced from 500ms)
            System.out.println("CAPTCHA Check (After Share click): " + getCaptchaStatus());
            
            // Wait for loader to disappear (wait for any loading indicator to be invisible)
            System.out.println("Waiting for loader to complete...");
            try {
                // Wait for common loader elements to disappear
                // Try multiple common loader XPaths
                String[] loaderXPaths = {
                    "//div[contains(@class,'loader')]",
                    "//div[contains(@class,'loading')]",
                    "//div[contains(@class,'spinner')]",
                    "//*[contains(@id,'loader')]",
                    "//*[contains(@id,'loading')]"
                };
                
                boolean loaderFound = false;
                for (String loaderXPath : loaderXPaths) {
                    try {
                        WebElement loader = driver.findElement(By.xpath(loaderXPath));
                        if (loader.isDisplayed()) {
                            loaderFound = true;
                            // Wait for loader to become invisible
                            wait.until(ExpectedConditions.invisibilityOf(loader));
                            System.out.println("Loader disappeared");
                            break;
                        }
                    } catch (Exception e) {
                        // Loader not found with this XPath, try next
                        continue;
                    }
                }
                
                if (!loaderFound) {
                    // If no loader found, wait a bit for any processing
                    Thread.sleep(1000); // Reduced from 2000ms
                }
            } catch (Exception e) {
                // If loader wait fails, continue with fixed wait
                System.out.println("Loader wait timeout, continuing...");
                Thread.sleep(1000); // Reduced from 2000ms
            }
            
            // Wait for success message "SMS sent successfully!" — up to 2 minutes total
            System.out.println("Waiting for success message: 'SMS sent successfully!' (max 2 minutes)...");
            String successMessageText = null;
            long successWaitStartTime = System.currentTimeMillis();
            long successWaitTimeoutMs = Duration.ofMinutes(2).toMillis();
            boolean messageFound = false;
            String[] successMessageXPaths = {
                "//*[contains(text(),'SMS sent successfully')]",
                "//*[contains(text(),'SMS sent successfully!')]",
                "//*[contains(text(),'SMS Sent Successfully')]",
                "//*[contains(text(),'SMS Sent Successfully!')]",
                "//div[contains(text(),'SMS sent successfully')]",
                "//span[contains(text(),'SMS sent successfully')]",
                "//p[contains(text(),'SMS sent successfully')]"
            };
            // Known failure messages the share forms render in place of the success text.
            // Without these, a rejected submit is indistinguishable from a slow one: both
            // just burn the full 2 minutes and report "taking too long".
            String[] failureMessages = {
                "Monthly SMS limit reached",
                "Failed to send SMS",
                "An error occurred. Please try again",
                "An unexpected issue occurred. Please try again",
                "You must agree to receive SMS messages",
                "Please enter a valid 10-digit phone number",
                "Please check the reCAPTCHA checkbox"
            };
            String failureMessageText = null;

            while (!messageFound && (System.currentTimeMillis() - successWaitStartTime) < successWaitTimeoutMs) {
                for (String messageXPath : successMessageXPaths) {
                    try {
                        WebElement successMessage = driver.findElement(By.xpath(messageXPath));
                        if (successMessage != null && successMessage.isDisplayed()) {
                            successMessageText = successMessage.getText();
                            System.out.println("Success message found: " + successMessageText);
                            messageFound = true;
                            Thread.sleep(1000);
                            System.out.println("CAPTCHA Check (Final - After success): " + getCaptchaStatus());
                            break;
                        }
                    } catch (org.openqa.selenium.NoSuchElementException e) {
                        // Element not found, try next XPath
                        continue;
                    } catch (Exception e) {
                        // Other error, try next XPath
                        continue;
                    }
                }
                if (!messageFound) {
                    for (String failure : failureMessages) {
                        try {
                            WebElement failureEl = driver.findElement(
                                By.xpath("//*[contains(text(),'" + failure + "')]"));
                            if (failureEl != null && failureEl.isDisplayed()) {
                                failureMessageText = failureEl.getText().trim();
                                System.out.println("Failure message found: " + failureMessageText);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                if (!messageFound && failureMessageText != null) {
                    break; // site rejected the submit - no point waiting out the timeout
                }
                if (!messageFound) {
                    Thread.sleep(500); // Small delay before checking again
                }
            }
            if (!messageFound && (System.currentTimeMillis() - successWaitStartTime) >= successWaitTimeoutMs) {
                System.out.println("Success message wait timeout after 2 minutes");
            }
            if (!messageFound) {
                long duration = System.currentTimeMillis() - startTime;
                String errorMsg = failureMessageText != null
                    ? "Site rejected the submit: \"" + failureMessageText + "\""
                    : "Success message not shown within 2 minutes - taking too long or submit did not complete";
                System.err.println("\n==========================================");
                System.err.println("❌ " + errorMsg);
                System.err.println("TEST RESULT: FAIL for " + url);
                System.err.println("==========================================\n");
                result.status = "FAIL";
                result.errorMessage = errorMsg;
                result.captchaStatus = getCaptchaStatus();
                result.duration = duration;
                return result;
            }
            
            // Final CAPTCHA status summary
            String finalCaptchaStatus = getCaptchaStatus();
            System.out.println("\n--- CAPTCHA Status Summary ---");
            System.out.println(finalCaptchaStatus);
            if (isCaptchaPresent()) {
                System.out.println("⚠️  CAPTCHA is present on the page");
            } else {
                System.out.println("✓ CAPTCHA is not present");
            }
            
            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;
            result.status = "PASS";
            result.captchaStatus = finalCaptchaStatus;
            result.successMessage = successMessageText != null ? successMessageText : "Not found";
            result.duration = duration;
            
            System.out.println("\n==========================================");
            System.out.println("TEST RESULT: PASS for " + url);
            System.out.println("==========================================\n");
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = e.getMessage();
            
            System.err.println("\n==========================================");
            System.err.println("TEST RESULT: FAIL for " + url);
            System.err.println("Error: " + errorMsg);
            System.err.println("==========================================\n");
            e.printStackTrace();
            
            // Update result
            result.status = "FAIL";
            result.errorMessage = errorMsg;
            result.duration = duration;
            result.captchaStatus = getCaptchaStatus();
            
            return result;
        }
    }
    
    /**
     * Cleanup method to close browser
     */
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("Browser closed successfully");
        }
    }
    
    /**
     * Main method to run the test for all URLs
     * Usage: 
     *   - No arguments: Test all URLs
     *   - "oklahoma": Test only Oklahoma
     *   - "westvirginia": Test only West Virginia
     *   - "oregon": Test only Oregon
     *   - "delaware": Test only Delaware
     *   - "indiana": Test only Indiana
     *   - "newjersey": Test only New Jersey
     *   - "connecticut": Test only Connecticut
     *   - "livewell": Test only Livewell
     *   - Any URL: Test that specific URL
     */
    public static void main(String[] args) {
        SMSShareTest test = new SMSShareTest();
        
        // Initialize Slack Service if configured
        if (!SLACK_WEBHOOK_URL.isEmpty()) {
            try {
                slackService = new SlackService(SLACK_WEBHOOK_URL, 
                    SLACK_CHANNEL.isEmpty() ? null : SLACK_CHANNEL,
                    SLACK_USERNAME.isEmpty() ? null : SLACK_USERNAME);
                if (slackService.isEnabled()) {
                    System.out.println("✓ Slack notifications enabled");
                    System.out.println("  Webhook URL: " + SLACK_WEBHOOK_URL.substring(0, Math.min(50, SLACK_WEBHOOK_URL.length())) + "...");
                    if (!SLACK_CHANNEL.isEmpty()) {
                        System.out.println("  Channel: " + SLACK_CHANNEL);
                    }
                    if (!SLACK_USERNAME.isEmpty()) {
                        System.out.println("  Username: " + SLACK_USERNAME);
                    }
                } else {
                    System.out.println("⚠️  Warning: Invalid Slack webhook URL");
                    System.out.println("   Slack notifications will be disabled.");
                }
            } catch (Exception e) {
                System.err.println("⚠️  Warning: Failed to initialize Slack service: " + e.getMessage());
                System.err.println("   Slack notifications will be disabled.");
            }
        } else {
            System.out.println("ℹ️  Slack notifications disabled (no webhook URL provided)");
            System.out.println("   To enable: -Dslack.webhook.url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL");
        }
        
        // Determine which URLs to test
        String[] urlsToTest;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("oklahoma")) {
                urlsToTest = new String[]{"https://oklahoma.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("westvirginia")) {
                urlsToTest = new String[]{"https://westvirginia.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("oregon")) {
                urlsToTest = new String[]{"https://oregon.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("delaware")) {
                urlsToTest = new String[]{"https://delaware.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("indiana")) {
                urlsToTest = new String[]{"https://indiana.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("indiana-breastfeeding") || args[0].equalsIgnoreCase("infographic")) {
                urlsToTest = new String[]{"https://indiana.wicresources.org/breastfeeding/"};
            } else if (args[0].equalsIgnoreCase("newjersey")) {
                urlsToTest = new String[]{"https://newjersey.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("connecticut")) {
                urlsToTest = new String[]{"https://connecticut.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("livewell")) {
                urlsToTest = new String[]{"https://livewell.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("kansas")) {
                urlsToTest = new String[]{"https://kansaswic.wicresources.org/approved-food-list/"};
            } else if (args[0].equalsIgnoreCase("nebraska")) {
                urlsToTest = new String[]{"https://nebraska.wicresources.org/"};
            } else if (args[0].equalsIgnoreCase("chickasaw")) {
                urlsToTest = new String[]{"https://chickasawnation.ebtresources.org/summer-ebt-approved-food-list/"};
            } else if (args[0].startsWith("http")) {
                urlsToTest = new String[]{args[0]};
            } else {
                System.out.println("Invalid argument. Use 'oklahoma', 'westvirginia', 'oregon', 'delaware', 'indiana', 'indiana-breastfeeding', 'infographic', 'kansas', 'nebraska', 'chickasaw', 'newjersey', 'connecticut', 'livewell', or a full URL.");
                return;
            }
        } else {
            urlsToTest = TEST_URLS;
        }
        
        // Track test results
        int totalTests = urlsToTest.length;
        int passedTests = 0;
        int failedTests = 0;
        int captchaErrorCount = 0;
        java.util.List<TestResult> allResults = new java.util.ArrayList<>();
        
        try {
            // Setup WebDriver once for all tests
            System.out.println("Initializing WebDriver...");
            test.setUp();
            
            // Execute test for each URL
            System.out.println("\nStarting tests for " + totalTests + " URL(s)...\n");
            
            for (int i = 0; i < urlsToTest.length; i++) {
                String url = urlsToTest[i];
                System.out.println("\n[" + (i + 1) + "/" + totalTests + "] Testing: " + url);
                
                TestResult result = test.testSMSShareForm(url);
                allResults.add(result);
                
                if (result.status.equals("PASS")) {
                    passedTests++;
                } else if (result.status.equals("CAPTCHA_ERROR")) {
                    captchaErrorCount++;
                } else {
                    failedTests++;
                }
                
                // Small delay between tests
                if (i < urlsToTest.length - 1) {
                    Thread.sleep(1000); // Reduced from 2000ms for faster execution
                }
            }
            
            // Calculate success rate (PASS only)
            double successRate = (passedTests * 100.0 / totalTests);
            
            // Print final summary
            System.out.println("\n\n");
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║              TEST EXECUTION SUMMARY                   ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.println("║  Total URLs Tested: " + String.format("%-30s", totalTests) + "║");
            System.out.println("║  Passed:            " + String.format("%-30s", passedTests) + "║");
            System.out.println("║  Failed:            " + String.format("%-30s", failedTests) + "║");
            System.out.println("║  CAPTCHA Error:     " + String.format("%-30s", captchaErrorCount) + "║");
            System.out.println("║  Success Rate:      " + String.format("%-28.1f", successRate) + "%║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            
            if (failedTests == 0 && captchaErrorCount == 0) {
                System.out.println("\n✅ All tests PASSED!");
            } else if (captchaErrorCount > 0 && failedTests == 0) {
                System.out.println("\n⚠️  Some URLs stopped due to CAPTCHA error (site-automation check).");
            } else if (failedTests > 0) {
                System.out.println("\n❌ Some tests FAILED. Check Slack/logs for details.");
            } else {
                System.out.println("\n⚠️  Mix of passed, failed, and CAPTCHA-error. Check Slack/logs.");
            }
            
            // Send Slack notification with test results if enabled
            if (slackService != null && slackService.isEnabled()) {
                // Convert TestResult objects to SlackService.TestResult objects
                List<SlackService.TestResult> slackResults = new ArrayList<>();
                for (TestResult r : allResults) {
                    slackResults.add(new SlackService.TestResult(
                        r.url,
                        r.status,
                        r.captchaStatus != null ? r.captchaStatus : "N/A",
                        r.successMessage != null ? r.successMessage : "N/A",
                        r.errorMessage != null ? r.errorMessage : "N/A",
                        r.duration
                    ));
                }
                
                // Send to Slack
                slackService.sendTestResults(totalTests, passedTests, failedTests, captchaErrorCount, successRate, slackResults);
            }
            
        } catch (Exception e) {
            System.err.println("Test execution failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            test.tearDown();
        }
    }
}

