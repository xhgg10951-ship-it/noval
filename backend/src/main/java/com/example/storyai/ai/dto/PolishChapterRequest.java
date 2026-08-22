package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request for {@code POST /ai/polish-chapter} (v0.1.1 Phase 8 / TASK-167).
 * Shape MUST match the Pydantic {@code PolishChapterRequest}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PolishChapterRequest(
        String content,
        String chapterGoal,
        String endingIntent,
        List<ConstraintItem> constraints,
        List<StateItem> currentState,
        String writingStyle,
        String userInstruction
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConstraintItem(String type, String content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StateItem(String category, String subject, String field, String value) {
    }
}
