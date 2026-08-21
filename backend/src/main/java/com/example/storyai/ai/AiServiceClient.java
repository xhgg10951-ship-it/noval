package com.example.storyai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;

/**
 * Unified client for the Python AI Service.
 *
 * <p>All backend→Python HTTP calls go through here so business Services never
 * hand-write HTTP (architecture §26).</p>
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

    /** Calls {@code POST /ai/plan-stage} — initial stage planning (TASK-015). */
    public PlanStageResponse planStage(PlanStageRequest request) {
        return restClient.post()
                .uri("/ai/plan-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PlanStageResponse.class);
    }

    /** Calls {@code POST /ai/replan-stage} — full re-planning (TASK-016, AT-B02). */
    public PlanStageResponse replanStage(PlanStageRequest request) {
        return restClient.post()
                .uri("/ai/replan-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PlanStageResponse.class);
    }
}
