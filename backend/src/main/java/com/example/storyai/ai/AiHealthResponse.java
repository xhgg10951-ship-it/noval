package com.example.storyai.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of the Python AI Service {@code /health} response.
 *
 * <p>{@code mock_llm} is serialized snake_case by FastAPI; mapped to camelCase
 * here so Java conventions are respected on our side.
 */
public record AiHealthResponse(
        String status,
        String version,
        @JsonProperty("mock_llm") boolean mockLlm
) {
}
