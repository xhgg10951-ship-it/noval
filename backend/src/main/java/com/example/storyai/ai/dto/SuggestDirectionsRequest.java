package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request sent to the Python Planner Suggestion endpoint (M6 / TASK-042).
 *
 * <p>Shape MUST match Pydantic {@code SuggestDirectionsRequest} in
 * {@code ai-service/app/schemas/models.py}. Reuses the nested records from
 * {@link PlanStageRequest}/{@link GenerateChapterRequest} so the JSON keys stay
 * identical to the contract.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuggestDirectionsRequest(
        @JsonProperty("coreIdea") String coreIdea,
        @JsonProperty("constraints") List<PlanStageRequest.ConstraintItem> constraints,
        @JsonProperty("currentState") List<PlanStageRequest.StateItem> currentState,
        @JsonProperty("storyMemories") List<PlanStageRequest.MemoryItem> storyMemories,
        @JsonProperty("relationshipState") List<GenerateChapterRequest.RelationshipItem> relationshipState,
        @JsonProperty("recentContext") String recentContext
) {
}
