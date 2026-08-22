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
        Integer targetChapterCount,
        // ---- TASK-107: Planner continuation context v2 ----
        List<RelationshipItem> relationshipState,
        Integer currentChapterNumber,
        List<String> completedStageSummaries,
        List<String> recentChapterSummaries,
        ContinuationAnchor continuationAnchor,
        // ---- v0.1.1 Phase 6 (TASK-154): long-form position ----
        LongFormPosition longFormPosition
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RelationshipItem(String subjectA, String subjectB, String description) {
    }

    /**
     * TASK-108 — structured continuation anchor. {@code lastChapterNumber},
     * {@code lastChapterSummary} and {@code lastChapterEnding} are populated by
     * the context reader; {@code currentLocation} / {@code activeCharacters} /
     * {@code currentImmediateGoal} are derived (see StoryContextReader).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContinuationAnchor(
            Integer lastChapterNumber,
            String currentLocation,
            List<String> activeCharacters,
            String currentImmediateGoal,
            String lastChapterSummary,
            String lastChapterEnding
    ) {
    }

    /**
     * TASK-154 — long-form position: where this story sits within its total
     * length and its current arc. The pace guard (TASK-155) reasons over these
     * numbers; all fields optional so short stories simply omit the block.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LongFormPosition(
            Integer targetChapterCount,
            Integer currentChapterNumber,
            String arcTitle,
            String arcGoal,
            Integer arcStartChapter,
            Integer arcEndChapter
    ) {
    }
}
