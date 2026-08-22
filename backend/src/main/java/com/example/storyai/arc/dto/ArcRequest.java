package com.example.storyai.arc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating/updating an arc (v0.1.1 Phase 6 / TASK-152).
 * Null fields on update mean "leave unchanged".
 */
public class ArcRequest {

    @NotBlank(message = "卷标题不能为空")
    private String title;

    private String goal;

    @NotNull(message = "起始章节不能为空")
    @Min(value = 1, message = "起始章节至少为 1")
    private Integer targetStartChapter;

    @NotNull(message = "结束章节不能为空")
    @Max(value = 5000, message = "结束章节过大（上限 5000）")
    private Integer targetEndChapter;

    /** PLANNED / ACTIVE / COMPLETED; setting ACTIVE clears any other active arc. */
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public Integer getTargetStartChapter() {
        return targetStartChapter;
    }

    public void setTargetStartChapter(Integer targetStartChapter) {
        this.targetStartChapter = targetStartChapter;
    }

    public Integer getTargetEndChapter() {
        return targetEndChapter;
    }

    public void setTargetEndChapter(Integer targetEndChapter) {
        this.targetEndChapter = targetEndChapter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
