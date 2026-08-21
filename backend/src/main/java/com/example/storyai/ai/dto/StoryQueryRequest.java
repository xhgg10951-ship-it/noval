package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request sent to the Python Story Query endpoint (M6 / TASK-044).
 *
 * <p>Shape MUST match Pydantic {@code StoryQueryRequest} in
 * {@code ai-service/app/schemas/models.py}. Carries the question plus the
 * assembled Current State / Relationships / Story Memories / recent context.
 * No RAG is added (M6 scope) — only structured, already-persisted story info.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryQueryRequest(
        @JsonProperty("question") String question,
        @JsonProperty("currentState") List<PlanStageRequest.StateItem> currentState,
        @JsonProperty("storyMemories") List<PlanStageRequest.MemoryItem> storyMemories,
        @JsonProperty("relationshipState") List<GenerateChapterRequest.RelationshipItem> relationshipState,
        @JsonProperty("recentContext") String recentContext,
        @JsonProperty("sourceEvidence") List<PlanStageRequest.MemoryItem> sourceEvidence
) {
}
