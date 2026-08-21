package com.example.storyai.stage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/stories/{storyId}/stages} — create a new
 * stage and generate its initial plan (AT-B01).
 */
public class CreateStageRequest {

    @NotBlank(message = "阶段方向不能为空")
    private String direction;

    @Min(value = 1, message = "目标章节数至少为 1")
    private Integer targetChapterCount;

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getTargetChapterCount() {
        return targetChapterCount;
    }

    public void setTargetChapterCount(Integer targetChapterCount) {
        this.targetChapterCount = targetChapterCount;
    }
}
