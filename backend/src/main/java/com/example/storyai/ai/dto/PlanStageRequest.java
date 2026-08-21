package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured request for Python {@code POST /ai/plan-stage} and
 * {@code POST /ai/replan-stage} (TASK-013).
 *
 * <p>Field names and shapes MUST match the Pydantic models in
 * {@code ai-service/app/schemas/models.py} — Java never regex-parses natural
 * language planner output (ARCHITECTURE).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlanStageRequest(
        String coreIdea,
        List<ConstraintItem> constraints,
        String stageDirection,
        List<StateItem> currentState,
        List<MemoryItem> storyMemories,
        String recentContext,
        Integer targetChapterCount
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConstraintItem(String type, String content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StateItem(String category, String subject, String field, String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MemoryItem(String type, String subject, String description) {
    }
}
