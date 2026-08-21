package com.example.storyai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.ExtractMemoryRequest;
import com.example.storyai.ai.dto.ExtractMemoryResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unified client for the Python AI Service.
 *
 * <p>All backend→Python HTTP calls go through here so business Services never
 * hand-write HTTP (architecture §26).</p>
 *
 * <p>The request body is serialized to JSON with the Spring-managed
 * {@link ObjectMapper} and sent as an explicit {@code String} payload so
 * {@code StringHttpMessageConverter} sets a correct {@code Content-Length}.</p>
 *
 * <p>The RestClient is built on a plain {@link SimpleClientHttpRequestFactory}
 * (HttpURLConnection, HTTP/1.1). This is deliberate: Spring Boot's auto-configured
 * {@code RestClient.Builder} bean defaults to the JDK {@code java.net.http.HttpClient},
 * which sends {@code Upgrade: h2c} to negotiate HTTP/2. FastAPI/uvicorn (h11) does
 * not handle that upgrade on a cleartext connection and silently reads an empty
 * body -> 422. Forcing HTTP/1.1 via HttpURLConnection avoids the entire class of
 * problem.</p>
 */
@Component
public class AiServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(RestClient.Builder builder,
                           ObjectMapper objectMapper,
                           @Value("${ai.service.base-url:http://localhost:8000}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setOutputStreaming(false);
        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /** Probes the Python AI Service {@code /health} endpoint (TASK-006). */
    public AiHealthResponse checkHealth() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(AiHealthResponse.class);
    }

    /** Calls {@code POST /ai/plan-stage} — initial stage planning (TASK-015). */
    public PlanStageResponse planStage(PlanStageRequest request) {
        String json = serialize(request);
        return restClient.post()
                .uri("/ai/plan-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(PlanStageResponse.class);
    }

    /** Calls {@code POST /ai/replan-stage} — full re-planning (TASK-016, AT-B02). */
    public PlanStageResponse replanStage(PlanStageRequest request) {
        String json = serialize(request);
        return restClient.post()
                .uri("/ai/replan-stage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(PlanStageResponse.class);
    }

    /** Calls {@code POST /ai/generate-chapter} — single chapter generation (TASK-023, AT-C01). */
    public GenerateChapterResponse generateChapter(GenerateChapterRequest request) {
        String json = serialize(request);
        return restClient.post()
                .uri("/ai/generate-chapter")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(GenerateChapterResponse.class);
    }

    /** Calls {@code POST /ai/extract-memory} — memory candidate extraction (TASK-027, AT-G / AT-E). */
    public ExtractMemoryResponse extractMemory(ExtractMemoryRequest request) {
        String json = serialize(request);
        return restClient.post()
                .uri("/ai/extract-memory")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(ExtractMemoryResponse.class);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AI request: " + e.getMessage(), e);
        }
    }
}
