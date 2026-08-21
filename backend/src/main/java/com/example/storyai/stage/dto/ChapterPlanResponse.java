package com.example.storyai.stage.dto;

import java.time.LocalDateTime;

import com.example.storyai.stage.model.ChapterPlan;

/**
 * Response DTO for a single chapter plan.
 */
public class ChapterPlanResponse {

    private Long id;
    private Long stageId;
    private Integer chapterOrder;
    private String goal;
    private String expectedProgress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChapterPlanResponse() {
    }

    public ChapterPlanResponse(ChapterPlan plan) {
        this.id = plan.getId();
        this.stageId = plan.getStageId();
        this.chapterOrder = plan.getChapterOrder();
        this.goal = plan.getGoal();
        this.expectedProgress = plan.getExpectedProgress();
        this.createdAt = plan.getCreatedAt();
        this.updatedAt = plan.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Integer getChapterOrder() {
        return chapterOrder;
    }

    public void setChapterOrder(Integer chapterOrder) {
        this.chapterOrder = chapterOrder;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getExpectedProgress() {
        return expectedProgress;
    }

    public void setExpectedProgress(String expectedProgress) {
        this.expectedProgress = expectedProgress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
