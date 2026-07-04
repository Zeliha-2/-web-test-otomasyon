package com.automation.utils;

import com.automation.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public final class JiraClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private JiraClient() {
    }

    public static boolean isConfigured() {
        return ConfigManager.getBoolean("jira.enabled", false)
                && !blank(ConfigManager.get("jira.base.url"))
                && !blank(ConfigManager.get("jira.email"))
                && !blank(ConfigManager.get("jira.api.token"));
    }

    public static List<JiraDuplicateMatch> findDuplicates(String caseId, String summary, String errorMessage) {
        List<JiraDuplicateMatch> matches = new ArrayList<>();
        if (!isConfigured()) {
            return matches;
        }
        String jql = buildDuplicateJql(caseId, summary, errorMessage);
        try {
            String base = normalizeBaseUrl(ConfigManager.get("jira.base.url"));
            String project = ConfigManager.get("jira.project.key");
            String encodedJql = java.net.URLEncoder.encode(jql, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/rest/api/3/search?jql=" + encodedJql + "&maxResults=5&fields=summary,status"))
                    .header("Authorization", basicAuth())
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                System.out.println("[JIRA-WARN] duplicate search failed HTTP " + response.statusCode());
                return matches;
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode issues = root.path("issues");
            if (!issues.isArray()) {
                return matches;
            }
            for (JsonNode issue : issues) {
                JiraDuplicateMatch match = new JiraDuplicateMatch();
                match.issueKey = issue.path("key").asText("");
                match.summary = issue.path("fields").path("summary").asText("");
                match.status = issue.path("fields").path("status").path("name").asText("");
                match.url = base + "/browse/" + match.issueKey;
                matches.add(match);
            }
        } catch (Exception e) {
            System.out.println("[JIRA-WARN] duplicate search skipped: " + e.getMessage());
        }
        return matches;
    }

    public static JiraCreateResult createIssue(ObjectNode fields) throws IOException, InterruptedException {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("fields", fields);
        String base = normalizeBaseUrl(ConfigManager.get("jira.base.url"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/rest/api/3/issue"))
                .header("Authorization", basicAuth())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JiraCreateResult result = new JiraCreateResult();
        result.httpStatus = response.statusCode();
        result.rawBody = response.body();
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode json = MAPPER.readTree(response.body());
            result.issueKey = json.path("key").asText("");
            result.issueId = json.path("id").asText("");
            result.url = base + "/browse/" + result.issueKey;
            result.success = !result.issueKey.isBlank();
        }
        return result;
    }

    public static boolean attachScreenshot(String issueKey, Path screenshotFile) {
        if (!isConfigured() || issueKey == null || issueKey.isBlank() || screenshotFile == null) {
            return false;
        }
        if (!Files.isRegularFile(screenshotFile)) {
            System.out.println("[JIRA-WARN] screenshot not found: " + screenshotFile);
            return false;
        }
        try {
            String base = normalizeBaseUrl(ConfigManager.get("jira.base.url"));
            String boundary = "----QABoundary" + System.currentTimeMillis();
            byte[] fileBytes = Files.readAllBytes(screenshotFile);
            String fileName = screenshotFile.getFileName().toString();
            byte[] body = buildMultipart(boundary, fileName, fileBytes);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/rest/api/3/issue/" + issueKey + "/attachments"))
                    .header("Authorization", basicAuth())
                    .header("X-Atlassian-Token", "no-check")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.out.println("[JIRA-WARN] attachment failed: " + e.getMessage());
            return false;
        }
    }

    public static ObjectNode buildIssueFields(
            String projectKey,
            String summary,
            String description,
            String priorityName,
            String issueType) throws IOException {
        ObjectNode fields = MAPPER.createObjectNode();
        ObjectNode project = fields.putObject("project");
        project.put("key", projectKey);
        ObjectNode type = fields.putObject("issuetype");
        type.put("name", issueType == null || issueType.isBlank() ? "Bug" : issueType);
        fields.put("summary", summary);
        ObjectNode desc = fields.putObject("description");
        desc.put("type", "doc");
        desc.put("version", 1);
        ArrayNode content = desc.putArray("content");
        ObjectNode paragraph = content.addObject();
        paragraph.put("type", "paragraph");
        ArrayNode textArr = paragraph.putArray("content");
        ObjectNode text = textArr.addObject();
        text.put("type", "text");
        text.put("text", description == null ? "" : description);
        if (priorityName != null && !priorityName.isBlank() && !"none".equalsIgnoreCase(priorityName)) {
            ObjectNode priority = fields.putObject("priority");
            priority.put("name", mapPriority(priorityName));
        }
        return fields;
    }

    private static String mapPriority(String priority) {
        return switch (priority.toLowerCase(Locale.ROOT)) {
            case "highest", "critical" -> "Highest";
            case "high", "major" -> "High";
            case "low", "minor" -> "Low";
            case "lowest", "trivial" -> "Lowest";
            default -> "Medium";
        };
    }

    private static String buildDuplicateJql(String caseId, String summary, String errorMessage) {
        String project = ConfigManager.get("jira.project.key");
        StringBuilder jql = new StringBuilder("project = ");
        jql.append(project == null || project.isBlank() ? "QA" : project.trim());
        jql.append(" AND issuetype = Bug AND status != Done");
        if (caseId != null && !caseId.isBlank()) {
            jql.append(" AND (summary ~ \"").append(escapeJql(caseId)).append("\"");
            jql.append(" OR description ~ \"").append(escapeJql(caseId)).append("\")");
        } else if (summary != null && !summary.isBlank()) {
            String token = summary.length() > 40 ? summary.substring(0, 40) : summary;
            jql.append(" AND summary ~ \"").append(escapeJql(token)).append("\"");
        } else if (errorMessage != null && errorMessage.length() > 20) {
            jql.append(" AND description ~ \"").append(escapeJql(errorMessage.substring(0, 20))).append("\"");
        }
        jql.append(" ORDER BY created DESC");
        return jql.toString();
    }

    private static String escapeJql(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static byte[] buildMultipart(String boundary, String fileName, byte[] fileBytes) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
        return body;
    }

    private static String basicAuth() {
        String email = ConfigManager.get("jira.email");
        String token = ConfigManager.get("jira.api.token");
        String raw = email + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.trim().replaceAll("/+$", "");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public static class JiraDuplicateMatch {
        public String issueKey;
        public String summary;
        public String status;
        public String url;
    }

    public static class JiraCreateResult {
        public boolean success;
        public int httpStatus;
        public String issueKey = "";
        public String issueId = "";
        public String url = "";
        public String rawBody = "";
    }
}
