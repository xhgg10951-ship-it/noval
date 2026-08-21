package com.example.storyai.stage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/stages/{stageId}/replan} — regenerate the
 * complete plan for a new target chapter count (AT-B02).
 */
public class ReplanRequest {

    @NotNull(message = "目标章节数不能为空")
    @Min(value = 1, message = "目标章节数至少为 1")
    @Max(value = 50, message = "目标章节数过多（v0.1 上限 50）")
    private Integer targetChapterCount;

    public Integer getTargetChapterCount() {
        return targetChapterCount;
    }

    public void setTargetChapterCount(Integer targetChapterCount) {
        this.targetChapterCount = targetChapterCount;
    }
}
