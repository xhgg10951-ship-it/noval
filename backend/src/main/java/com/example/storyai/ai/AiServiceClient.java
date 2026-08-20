package com.example.storyai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Unified client for the Python AI Service.
 *
 * <p>All backend→Python HTTP calls go through here so business Services never
 * hand-write HTTP (architecture §26). For M0 it only proves the boundary with
 * a health check; planner/writer/extract/query calls are added in M2+.
 */
@Component
public class AiServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AiServiceClient(@Value("${ai.service.base-url:http://localhost:8000}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** Probes the Python AI Service {@code /health} endpoint. */
    public AiHealthResponse checkHealth() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(AiHealthResponse.class);
    }
}
