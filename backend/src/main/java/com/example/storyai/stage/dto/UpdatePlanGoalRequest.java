package com.example.storyai.stage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

/**
 * Request body for {@code PUT /api/stages/plans/{planId}} — author edits a
 * chapter goal (AT-B03).
 */
public class UpdatePlanGoalRequest {

    @NotBlank(message = "章节目标不能为空")
    private String goal;

    @Min(value = 300, message = "本章目标字数至少为 300")
    private Integer targetCharacters;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
    }
}
