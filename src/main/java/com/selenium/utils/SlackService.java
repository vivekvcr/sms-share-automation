package com.selenium.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Slack Service for sending test results to Slack via Incoming Webhooks
 * 
 * Setup: Create a Slack Incoming Webhook in your Slack workspace
 * Documentation: https://api.slack.com/messaging/webhooks
 */
public class SlackService {
    
    private String webhookUrl;
    private String channel;
    private String username;
    private boolean enabled;
    
    /**
     * Constructor
     * @param webhookUrl Slack Incoming Webhook URL
     * @param channel Optional channel name (e.g., #test-results). If null, uses webhook default
     * @param username Optional bot username. If null, uses webhook default
     */
    public SlackService(String webhookUrl, String channel, String username) {
        this.webhookUrl = webhookUrl;
        this.channel = channel;
        this.username = username;
        this.enabled = webhookUrl != null && !webhookUrl.isEmpty() && webhookUrl.startsWith("https://hooks.slack.com/");
    }
    
    /**
     * Constructor with just webhook URL (uses webhook defaults for channel and username)
     * @param webhookUrl Slack Incoming Webhook URL
     */
    public SlackService(String webhookUrl) {
        this(webhookUrl, null, null);
    }
    
    /**
     * Check if Slack service is enabled
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Send test results to Slack
     * @param totalTests Total number of tests
     * @param passedTests Number of passed tests
     * @param failedTests Number of failed tests
     * @param successRate Success rate percentage
     * @param testResults List of test result objects
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendTestResults(int totalTests, int passedTests, int failedTests, 
                                   double successRate, List<TestResult> testResults) {
        return sendTestResults(totalTests, passedTests, failedTests, 0, successRate, testResults);
    }
    
    /**
     * Send test results to Slack with CAPTCHA error count
     * @param totalTests Total number of tests
     * @param passedTests Number of passed tests
     * @param failedTests Number of failed tests
     * @param captchaErrorCount Number of CAPTCHA errors
     * @param successRate Success rate percentage
     * @param testResults List of test result objects
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendTestResults(int totalTests, int passedTests, int failedTests, 
                                   int captchaErrorCount, double successRate, List<TestResult> testResults) {
        if (!enabled) {
            System.out.println("ℹ️  Slack service disabled (missing or invalid webhook URL)");
            return false;
        }
        
        try {
            // Build Slack message payload
            String jsonPayload = buildSlackMessage(totalTests, passedTests, failedTests, captchaErrorCount, successRate, testResults);
            
            // Send to Slack
            return sendToSlack(jsonPayload);
            
        } catch (Exception e) {
            System.err.println("⚠️  Failed to send Slack notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Send a simple message to Slack
     * @param message Message text
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendMessage(String message) {
        if (!enabled) {
            return false;
        }
        
        try {
            String jsonPayload = String.format(
                "{\"text\":\"%s\"}",
                escapeJson(message)
            );
            return sendToSlack(jsonPayload);
        } catch (Exception e) {
            System.err.println("⚠️  Failed to send Slack message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Build Slack message payload with rich formatting
     */
    private String buildSlackMessage(int totalTests, int passedTests, int failedTests, 
                                    int captchaErrorCount, double successRate, List<TestResult> testResults) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Channel and username (optional)
        if (channel != null && !channel.isEmpty()) {
            json.append("\"channel\":\"").append(escapeJson(channel)).append("\",");
        }
        if (username != null && !username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        
        // Main message with blocks
        json.append("\"blocks\":[");
        
        // Header block with emoji based on results
        String headerEmoji = (failedTests == 0 && captchaErrorCount == 0) ? ":white_check_mark:" 
            : (failedTests > 0) ? ":x:" : ":warning:";
        String headerText = (failedTests == 0 && captchaErrorCount == 0) ? "SMS Share Test Results - All Passed!" 
            : (failedTests > 0) ? "SMS Share Test Results - Some Failed" 
            : "SMS Share Test Results - CAPTCHA Errors";
        
        json.append("{");
        json.append("\"type\":\"header\",");
        json.append("\"text\":{");
        json.append("\"type\":\"plain_text\",");
        json.append("\"text\":\"").append(headerEmoji).append(" ").append(headerText).append("\"");
        json.append("}");
        json.append("},");
        
        // Summary section: one line per stat with spacing between lines
        StringBuilder summaryText = new StringBuilder();
        int captchaNotPresentCount = 0;
        for (TestResult result : testResults) {
            if (result.captchaStatus != null && result.captchaStatus.contains("NOT PRESENT")) {
                captchaNotPresentCount++;
            }
        }
        summaryText.append("📊 Total Tests: ").append(totalTests).append("\n\n");
        summaryText.append("✅ Passed: ").append(passedTests).append("\n\n");
        summaryText.append("❌ Failed: ").append(failedTests).append("\n\n");
        if (captchaErrorCount > 0) {
            summaryText.append("⚠️ CAPTCHA Errors: ").append(captchaErrorCount).append("\n\n");
        }
        if (captchaNotPresentCount > 0) {
            summaryText.append("ℹ️ CAPTCHA Not Present: ").append(captchaNotPresentCount).append("\n\n");
        }
        summaryText.append("📈 Success Rate: ").append(String.format("%.1f%%", successRate));
        json.append("{");
        json.append("\"type\":\"section\",");
        json.append("\"text\":{");
        json.append("\"type\":\"mrkdwn\",");
        json.append("\"text\":\"").append(escapeJson(summaryText.toString())).append("\"");
        json.append("}");
        json.append("},");
        
        // Divider
        json.append("{\"type\":\"divider\"},");
        
        // Group results by status for better organization
        List<TestResult> passedResults = new java.util.ArrayList<>();
        List<TestResult> failedResults = new java.util.ArrayList<>();
        List<TestResult> captchaErrorResults = new java.util.ArrayList<>();
        List<TestResult> captchaNotPresentResults = new java.util.ArrayList<>();
        
        for (TestResult result : testResults) {
            if (result.captchaStatus != null && result.captchaStatus.contains("NOT PRESENT")) {
                captchaNotPresentResults.add(result);
            }
            if (result.status.equals("PASS")) {
                passedResults.add(result);
            } else if (result.status.equals("CAPTCHA_ERROR")) {
                captchaErrorResults.add(result);
            } else {
                failedResults.add(result);
            }
        }
        
        // Show FAILED results first (most important)
        if (!failedResults.isEmpty()) {
            json.append("{");
            json.append("\"type\":\"section\",");
            json.append("\"text\":{");
            json.append("\"type\":\"mrkdwn\",");
            json.append("\"text\":\"*❌ Failed Tests (").append(failedResults.size()).append("):*\"");
            json.append("}");
            json.append("},");
            
            int maxFailed = Math.min(failedResults.size(), 5);
            for (int i = 0; i < maxFailed; i++) {
                TestResult result = failedResults.get(i);
                json.append(buildTestResultBlock(result, false));
                if (i < maxFailed - 1) {
                    json.append(",");
                }
            }
            
            if (failedResults.size() > 5) {
                json.append(",");
                json.append("{");
                json.append("\"type\":\"context\",");
                json.append("\"elements\":[");
                json.append("{\"type\":\"mrkdwn\",\"text\":\"_... and ").append(failedResults.size() - 5).append(" more failed test(s)_\"}");
                json.append("]");
                json.append("}");
            }
            
            if (captchaErrorResults.size() > 0 || !captchaNotPresentResults.isEmpty() || passedResults.size() > 0) {
                json.append(",");
                json.append("{\"type\":\"divider\"},");
            }
        }
        
        // Show CAPTCHA_ERROR results
        if (!captchaErrorResults.isEmpty()) {
            json.append("{");
            json.append("\"type\":\"section\",");
            json.append("\"text\":{");
            json.append("\"type\":\"mrkdwn\",");
            json.append("\"text\":\"*⚠️ CAPTCHA Errors (").append(captchaErrorResults.size()).append("):*\"");
            json.append("}");
            json.append("},");
            
            int maxCaptcha = Math.min(captchaErrorResults.size(), 5);
            for (int i = 0; i < maxCaptcha; i++) {
                TestResult result = captchaErrorResults.get(i);
                json.append(buildTestResultBlock(result, false));
                if (i < maxCaptcha - 1) {
                    json.append(",");
                }
            }
            
            if (captchaErrorResults.size() > 5) {
                json.append(",");
                json.append("{");
                json.append("\"type\":\"context\",");
                json.append("\"elements\":[");
                json.append("{\"type\":\"mrkdwn\",\"text\":\"_... and ").append(captchaErrorResults.size() - 5).append(" more CAPTCHA error(s)_\"}");
                json.append("]");
                json.append("}");
            }
            
            if (!captchaNotPresentResults.isEmpty() || passedResults.size() > 0) {
                json.append(",");
                json.append("{\"type\":\"divider\"},");
            }
        }

        // Notify when CAPTCHA is not present
        if (!captchaNotPresentResults.isEmpty()) {
            json.append("{");
            json.append("\"type\":\"section\",");
            json.append("\"text\":{");
            json.append("\"type\":\"mrkdwn\",");
            json.append("\"text\":\"*ℹ️ CAPTCHA Not Present (").append(captchaNotPresentResults.size()).append("):*\"");
            json.append("}");
            json.append("},");

            int maxMissingCaptcha = Math.min(captchaNotPresentResults.size(), 5);
            for (int i = 0; i < maxMissingCaptcha; i++) {
                TestResult result = captchaNotPresentResults.get(i);
                json.append("{");
                json.append("\"type\":\"context\",");
                json.append("\"elements\":[");
                json.append("{\"type\":\"mrkdwn\",\"text\":\"• ").append(escapeJson(formatUrl(result.url))).append("\"}");
                json.append("]");
                json.append("}");
                if (i < maxMissingCaptcha - 1) {
                    json.append(",");
                }
            }

            if (captchaNotPresentResults.size() > 5) {
                json.append(",");
                json.append("{");
                json.append("\"type\":\"context\",");
                json.append("\"elements\":[");
                json.append("{\"type\":\"mrkdwn\",\"text\":\"_... and ").append(captchaNotPresentResults.size() - 5).append(" more URL(s)_\"}");
                json.append("]");
                json.append("}");
            }

            if (passedResults.size() > 0) {
                json.append(",");
                json.append("{\"type\":\"divider\"},");
            }
        }
        
        // Show PASSED results (collapsed if many)
        if (!passedResults.isEmpty()) {
            if (passedResults.size() <= 3) {
                // Show all passed results if 3 or fewer
                json.append("{");
                json.append("\"type\":\"section\",");
                json.append("\"text\":{");
                json.append("\"type\":\"mrkdwn\",");
                json.append("\"text\":\"*✅ Passed Tests (").append(passedResults.size()).append("):*\"");
                json.append("}");
                json.append("},");
                
                for (int i = 0; i < passedResults.size(); i++) {
                    TestResult result = passedResults.get(i);
                    json.append(buildTestResultBlock(result, true));
                    if (i < passedResults.size() - 1) {
                        json.append(",");
                    }
                }
            } else {
                // Show summary if many passed
                json.append("{");
                json.append("\"type\":\"section\",");
                json.append("\"text\":{");
                json.append("\"type\":\"mrkdwn\",");
                json.append("\"text\":\":white_check_mark: Passed Tests: *").append(passedResults.size()).append(" test(s) passed successfully*\"");
                json.append("}");
                json.append("}");
            }
        }
        
        // Footer with timestamp
        json.append(",");
        json.append("{");
        json.append("\"type\":\"context\",");
        json.append("\"elements\":[");
        json.append("{\"type\":\"mrkdwn\",\"text\":\":clock1: _Generated: ").append(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ).append("_\"}");
        json.append("]");
        json.append("}");
        
        json.append("]");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Build a formatted test result block
     * Pass results: one line (e.g. "✅ PASS - Connecticut - 12.3s")
     * Fail/CAPTCHA: multi-line with details
     */
    private String buildTestResultBlock(TestResult result, boolean isPassed) {
        StringBuilder block = new StringBuilder();
        block.append("{");
        block.append("\"type\":\"section\",");
        block.append("\"text\":{");
        block.append("\"type\":\"mrkdwn\",");
        
        String statusIcon = result.status.equals("PASS") ? ":white_check_mark:" 
            : result.status.equals("CAPTCHA_ERROR") ? ":warning:" : ":x:";
        
        StringBuilder detailText = new StringBuilder();
        
        if (isPassed && result.status.equals("PASS")) {
            // One line for pass: ✅ PASS - SiteName - duration
            String siteName = extractSiteName(result.url);
            String siteLabel = !siteName.isEmpty() ? siteName : formatUrl(result.url);
            detailText.append(statusIcon).append(" *PASS* • ").append(siteLabel)
                .append(" • ").append(formatDuration(result.duration));
        } else if (result.status.equals("CAPTCHA_ERROR")) {
            // Keep CAPTCHA_ERROR blocks short (URL + error only)
            detailText.append(":warning: CAPTCHA_ERROR - ").append(formatUrl(result.url));
        } else {
            // Multi-line for fail/CAPTCHA
            detailText.append(statusIcon).append(" *").append(result.status).append("*");
            detailText.append(" - ").append(formatUrl(result.url));
            detailText.append("\n");
            
            String siteName = extractSiteName(result.url);
            if (!siteName.isEmpty()) {
                detailText.append("📍 *").append(siteName).append("*");
                detailText.append("\n");
            }
            
            detailText.append("⏱️ Duration: ").append(formatDuration(result.duration));
            detailText.append("\n");
            
            if (!result.captchaStatus.equals("N/A") && !result.captchaStatus.isEmpty()) {
                String captchaIcon = result.captchaStatus.contains("NOT PRESENT") ? ":information_source:" : ":white_check_mark:";
                detailText.append(captchaIcon).append(" CAPTCHA: ").append(formatCaptchaStatus(result.captchaStatus));
                detailText.append("\n");
            }
            
            if (result.errorMessage != null && !result.errorMessage.equals("N/A")) {
                String errorPreview = result.errorMessage.length() > 150 
                    ? result.errorMessage.substring(0, 147) + "..." 
                    : result.errorMessage;
                detailText.append("\n");
                detailText.append("❌ Error: _").append(errorPreview).append("_");
            }
        }
        
        block.append("\"text\":\"").append(escapeJson(detailText.toString())).append("\"");
        block.append("}");
        block.append("}");
        
        return block.toString();
    }
    
    /**
     * Format duration from milliseconds to readable format
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }
    
    /**
     * Format URL for display (domain + optional path)
     */
    private String formatUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "N/A";
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                // Fallback: Extract domain name (e.g., "connecticut.wicresources.org")
                host = url.replace("https://", "").replace("http://", "").split("/")[0];
            }
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                return host + path;
            }
            return host;
        } catch (Exception e) {
            return truncateUrl(url);
        }
    }
    
    /**
     * Extract site name from URL (e.g., "Connecticut" from "connecticut.wicresources.org")
     */
    private String extractSiteName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            String domain = url.replace("https://", "").replace("http://", "").split("/")[0];
            String siteName = domain.split("\\.")[0];
            // Capitalize first letter
            if (!siteName.isEmpty()) {
                return siteName.substring(0, 1).toUpperCase() + siteName.substring(1);
            }
            return siteName;
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Format CAPTCHA status for display
     */
    private String formatCaptchaStatus(String captchaStatus) {
        if (captchaStatus == null || captchaStatus.isEmpty() || captchaStatus.equals("N/A")) {
            return "N/A";
        }
        // Preserve symbols and simplify text
        if (captchaStatus.contains("NOT PRESENT")) {
            return "Not Present (info)";
        } else if (captchaStatus.contains("PRESENT")) {
            // Extract type if available (reCAPTCHA, hCaptcha)
            String type = "";
            if (captchaStatus.contains("reCAPTCHA")) {
                type = "reCAPTCHA ";
            } else if (captchaStatus.contains("hCaptcha")) {
                type = "hCaptcha ";
            }
            // Check if visible or in DOM
            String visibility = captchaStatus.contains("visible") ? "(visible)" : "(in DOM)";
            return type + "Present ✓ " + visibility;
        }
        return captchaStatus.length() > 50 ? captchaStatus.substring(0, 47) + "..." : captchaStatus;
    }
    
    /**
     * Send JSON payload to Slack webhook
     */
    private boolean sendToSlack(String jsonPayload) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Write payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✓ Test results sent to Slack successfully");
                return true;
            } else {
                System.err.println("⚠️  Slack API returned error code: " + responseCode);
                // Try to read error message
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Error response: " + errorResponse.toString());
                }
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Error sending to Slack: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Truncate URL for display
     */
    private String truncateUrl(String url) {
        if (url == null) {
            return "N/A";
        }
        if (url.length() > 50) {
            return url.substring(0, 47) + "...";
        }
        return url;
    }
    
    /**
     * Test result data structure (matches SMSShareTest.TestResult)
     */
    public static class TestResult {
        public String url;
        public String status;
        public String captchaStatus;
        public String successMessage;
        public String errorMessage;
        public long duration;
        
        public TestResult(String url, String status, String captchaStatus, 
                         String successMessage, String errorMessage, long duration) {
            this.url = url;
            this.status = status;
            this.captchaStatus = captchaStatus;
            this.successMessage = successMessage;
            this.errorMessage = errorMessage;
            this.duration = duration;
        }
    }
}

