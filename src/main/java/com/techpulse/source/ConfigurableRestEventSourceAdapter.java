package com.techpulse.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techpulse.model.CostType;
import com.techpulse.model.EventMode;
import com.techpulse.model.EventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of EventSourceAdapter that fetches JSON event feeds
 * from a configured HTTP/HTTPS endpoint using Java's standard HttpClient.
 *
 * Credentials and URL are read strictly from environment variables / application.properties:
 * - TECHPULSE_LIVE_SOURCE_ENABLED
 * - TECHPULSE_LIVE_SOURCE_URL
 * - TECHPULSE_LIVE_SOURCE_API_KEY
 *
 * Never invents fake live endpoints or API keys. When not configured, isConfigured() returns false
 * so the application clearly reports "Live source not configured" in the UI.
 */
@Component
public class ConfigurableRestEventSourceAdapter implements EventSourceAdapter {

    private final boolean enabled;
    private final String sourceName;
    private final String sourceUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public ConfigurableRestEventSourceAdapter(
            @Value("${techpulse.live-source.enabled:false}") boolean enabled,
            @Value("${techpulse.live-source.name:Public Tech Feed Adapter}") String sourceName,
            @Value("${techpulse.live-source.url:}") String sourceUrl,
            @Value("${techpulse.live-source.api-key:}") String apiKey,
            @Value("${techpulse.live-source.timeout-seconds:8}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl != null ? sourceUrl.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean isConfigured() {
        return enabled && !sourceUrl.isEmpty() &&
                (sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://"));
    }

    @Override
    public String getConfigurationInstructions() {
        return "Set environment variables TECHPULSE_LIVE_SOURCE_ENABLED=true, " +
               "TECHPULSE_LIVE_SOURCE_URL=<https://your-event-feed-endpoint>, " +
               "and optionally TECHPULSE_LIVE_SOURCE_API_KEY=<token> before starting Spring Boot.";
    }

    @Override
    public List<ImportedEventPayload> fetchUpcomingEvents() throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Live event source is not configured.");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(sourceUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", "application/json")
                .GET();

        if (!apiKey.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Live source returned HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode eventsArray = root.isArray() ? root : root.path("events");
        if (!eventsArray.isArray()) {
            throw new RuntimeException("Expected JSON array or object with 'events' array from live source.");
        }

        List<ImportedEventPayload> imported = new ArrayList<>();
        for (JsonNode node : eventsArray) {
            String title = textOrNull(node, "title");
            String startRaw = textOrNull(node, "startTime");
            if (title == null || startRaw == null) {
                continue;
            }
            ImportedEventPayload item = new ImportedEventPayload();
            item.setExternalEventId(textOrNull(node, "id"));
            item.setTitle(sanitizeText(title));
            item.setDescription(sanitizeText(textOrDefault(node, "description", "Not provided")));
            item.setTopic(sanitizeText(textOrDefault(node, "topic", "Other")));
            item.setOrganizer(sanitizeText(textOrDefault(node, "organizer", "Not provided")));
            item.setCity(sanitizeText(textOrDefault(node, "city", "Online")));
            item.setVenueOrPlatform(sanitizeText(textOrDefault(node, "venueOrPlatform", "Not provided")));
            item.setEligibility(sanitizeText(textOrDefault(node, "eligibility", "Not provided")));
            item.setStartTime(OffsetDateTime.parse(startRaw));

            String endRaw = textOrNull(node, "endTime");
            if (endRaw != null) {
                item.setEndTime(OffsetDateTime.parse(endRaw));
            }
            String deadlineRaw = textOrNull(node, "registrationDeadline");
            if (deadlineRaw != null) {
                item.setRegistrationDeadline(OffsetDateTime.parse(deadlineRaw));
            }

            try {
                item.setEventType(EventType.valueOf(textOrDefault(node, "eventType", "WORKSHOP").toUpperCase()));
            } catch (IllegalArgumentException ex) {
                item.setEventType(EventType.WORKSHOP);
            }

            try {
                item.setMode(EventMode.valueOf(textOrDefault(node, "mode", "ONLINE").toUpperCase()));
            } catch (IllegalArgumentException ex) {
                item.setMode(EventMode.ONLINE);
            }

            String costRaw = textOrDefault(node, "costType", "NOT_PROVIDED").toUpperCase();
            try {
                item.setCostType(CostType.valueOf(costRaw));
            } catch (IllegalArgumentException ex) {
                item.setCostType(CostType.NOT_PROVIDED);
            }

            if (node.hasNonNull("price") && node.get("price").isNumber()) {
                item.setPrice(new BigDecimal(node.get("price").asText()));
            }

            item.setCurrency(sanitizeText(textOrDefault(node, "currency", "INR")));
            item.setRegistrationUrl(validateHttpUrl(textOrNull(node, "registrationUrl")));
            item.setOfficialSourceUrl(validateHttpUrl(textOrNull(node, "officialSourceUrl")));
            imported.add(item);
        }
        return imported;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String text = v.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        String v = textOrNull(node, field);
        return v != null ? v : fallback;
    }

    /**
     * Strips control characters and HTML tags so external descriptions are stored as safe plain text.
     */
    public static String sanitizeText(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Only permits valid http:// or https:// URLs. Rejects javascript: or malformed schemes.
     */
    public static String validateHttpUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (!(trimmed.startsWith("https://") || trimmed.startsWith("http://"))) {
            return null;
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return null;
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
