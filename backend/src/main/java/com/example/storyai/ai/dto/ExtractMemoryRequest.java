package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request sent to the Python Memory Extractor (TASK-027).
 *
 * <p>Shape MUST match the Pydantic {@code ExtractMemoryRequest} in
 * {@code ai-service/app/schemas/models.py}. Reuses the {@code ConstraintItem}
 * and {@code StateItem} nested records from {@link PlanStageRequest} so the
 * JSON shape stays identical (those are the contract, not the class name).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractMemoryRequest(
        @JsonProperty("chapterContent") String chapterContent,
        @JsonProperty("chapterSummary") String chapterSummary,
        @JsonProperty("chapterOrder") int chapterOrder,
        @JsonProperty("existingState") List<PlanStageRequest.StateItem> existingState,
        @JsonProperty("constraints") List<PlanStageRequest.ConstraintItem> constraints
) {
}
