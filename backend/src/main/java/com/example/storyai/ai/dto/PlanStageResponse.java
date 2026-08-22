package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured response from the Python planner (TASK-013).
 *
 * <pre>
 * suggestedChapterCount: the planner's recommended chapter count
 * chapterPlans[]:        ordered chapter goals with expected progress
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanStageResponse(
        int suggestedChapterCount,
        List<ChapterPlanItem> chapterPlans
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterPlanItem(
            int order,
            String goal,
            @com.fasterxml.jackson.annotation.JsonProperty("expectedProgress")
            String expectedProgress,
            // ---- TASK-115: ChapterSpec output ----
            Integer targetCharacters,
            String mustAdvance,
            String mustNotDo,
            String storyBeats,
            String endingIntent
    ) {
    }
}
