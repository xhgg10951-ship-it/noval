package com.example.storyai.stage.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /api/stages/plans/{planId}} — author edits a
 * chapter goal (AT-B03).
 */
public class UpdatePlanGoalRequest {

    @NotBlank(message = "章节目标不能为空")
    private String goal;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
