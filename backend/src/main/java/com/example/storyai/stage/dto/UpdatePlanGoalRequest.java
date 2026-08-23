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
    private String expectedProgress;
    private String mustAdvance;
    private String mustNotDo;
    private String storyBeats;
    private String endingIntent;

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

    public String getExpectedProgress() { return expectedProgress; }
    public void setExpectedProgress(String expectedProgress) { this.expectedProgress = expectedProgress; }
    public String getMustAdvance() { return mustAdvance; }
    public void setMustAdvance(String mustAdvance) { this.mustAdvance = mustAdvance; }
    public String getMustNotDo() { return mustNotDo; }
    public void setMustNotDo(String mustNotDo) { this.mustNotDo = mustNotDo; }
    public String getStoryBeats() { return storyBeats; }
    public void setStoryBeats(String storyBeats) { this.storyBeats = storyBeats; }
    public String getEndingIntent() { return endingIntent; }
    public void setEndingIntent(String endingIntent) { this.endingIntent = endingIntent; }
}
