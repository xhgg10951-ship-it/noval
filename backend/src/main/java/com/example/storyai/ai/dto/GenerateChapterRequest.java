package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Structured request for Python {@code POST /ai/generate-chapter} (TASK-020).
 *
 * <p>Field names and shapes MUST match the Pydantic {@code GenerateChapterRequest}
 * in {@code ai-service/app/schemas/models.py}. Java never regex-parses natural
 * language output — it validates these models.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateChapterRequest(
        String coreIdea,
        List<ConstraintItem> constraints,
        LongFormPosition longFormPosition,
        CurrentArc currentArc,
        String stageDirection,
        String chapterGoal,
        String expectedProgress,
        int chapterOrder,
        List<StateItem> currentState,
        List<MemoryItem> storyMemories,
        List<RelationshipItem> relationshipState,
        String recentContext,
        // ---- TASK-117: full ChapterSpec ----
        Integer targetCharacters,
        String mustAdvance,
        String mustNotDo,
        String storyBeats,
        String endingIntent,
        // ---- v0.1.1 Phase 8 (TASK-166): author writing style ----
        String writingStyle
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LongFormPosition(Integer targetChapterCount, Integer currentChapterNumber) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CurrentArc(
            String title,
            String goal,
            Integer targetStartChapter,
            Integer targetEndChapter) {
    }
}
