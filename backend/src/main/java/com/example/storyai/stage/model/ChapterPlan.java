package com.example.storyai.stage.model;

/**
 * ChapterPlan entity — one planned chapter within a stage (TASK-012).
 * {@code chapterOrder} is the planner-assigned sequence (1..N);
 * {@code goal} is author-editable (AT-B03).
 */
public class ChapterPlan {

    private Long id;
    private Long stageId;
    private Integer chapterOrder;
    private String goal;
    private String expectedProgress;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

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

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
