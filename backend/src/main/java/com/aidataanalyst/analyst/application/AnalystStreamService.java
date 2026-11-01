package com.aidataanalyst.analyst.application;

import com.aidataanalyst.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class AnalystStreamService {

    private static final Logger log = LoggerFactory.getLogger(AnalystStreamService.class);
    private static final byte[] UNAVAILABLE = """
            event: error
            data: {"message":"The analyst engine is unavailable."}

            """.getBytes(StandardCharsets.UTF_8);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public AnalystStreamService(HttpClient httpClient, ObjectMapper objectMapper, AppProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public StreamingResponseBody stream(UUID tenantId, UUID userId, String message) {
        return output -> {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("message", message);
            payload.put("tenantId", tenantId.toString());
            payload.put("userId", userId.toString());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.aiEngine().baseUrl()) + "/internal/v1/analyst/stream"))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("X-Internal-Api-Key", properties.aiEngine().apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    log.warn("Analyst engine returned status {}", response.statusCode());
                    output.write(UNAVAILABLE);
                    output.flush();
                    return;
                }
                try (InputStream body = response.body()) {
                    body.transferTo(output);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                writeUnavailable(output);
            } catch (IOException ex) {
                log.warn("Analyst stream failed: {}", ex.toString());
                writeUnavailable(output);
            }
        };
    }

    private void writeUnavailable(java.io.OutputStream output) {
        try {
            output.write(UNAVAILABLE);
            output.flush();
        } catch (IOException ignored) {
            // The client closed the stream.
        }
    }

    private String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
