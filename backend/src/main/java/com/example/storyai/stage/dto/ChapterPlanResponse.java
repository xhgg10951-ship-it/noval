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
    private Integer targetCharacters;
    // ---- TASK-133/136: plan versioning surfaced to the UI ----
    private Integer planVersion;
    private Boolean active;
    private String status;
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
        this.targetCharacters = plan.getTargetCharacters();
        this.planVersion = plan.getPlanVersion();
        this.active = plan.getActive();
        this.status = plan.getStatus();
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

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
    }

    public Integer getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(Integer planVersion) {
        this.planVersion = planVersion;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
