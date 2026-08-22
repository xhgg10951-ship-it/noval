package com.example.storyai.stage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/stages/{stageId}/replan-remaining}
 * (v0.1.1 Phase 4 — Replan Remaining, TASK-136).
 *
 * <p>Only the FUTURE is rewritten: completed plans are preserved, the still-active
 * remainder is superseded, and a new version of the remaining plans is generated
 * from the current story state.</p>
 */
public class ReplanRemainingRequest {

    /** How many chapters the REPLANNED remainder should contain. */
    @NotNull(message = "剩余章节数不能为空")
    @Min(value = 1, message = "剩余章节数至少为 1")
    @Max(value = 50, message = "剩余章节数过多（v0.1 上限 50）")
    private Integer remainingChapterCount;

    /** Optional author steering for the new remainder (appended to stage direction). */
    private String authorInstruction;

    public Integer getRemainingChapterCount() {
        return remainingChapterCount;
    }

    public void setRemainingChapterCount(Integer remainingChapterCount) {
        this.remainingChapterCount = remainingChapterCount;
    }

    public String getAuthorInstruction() {
        return authorInstruction;
    }

    public void setAuthorInstruction(String authorInstruction) {
        this.authorInstruction = authorInstruction;
    }
}
