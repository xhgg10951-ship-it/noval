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
    // ---- TASK-114: ChapterSpec ----
    private Integer targetCharacters;
    private String mustAdvance;
    private String mustNotDo;
    private String storyBeats;
    private String endingIntent;
    // ---- TASK-133: plan versioning / active flag (Replan Remaining) ----
    private Integer planVersion;   // increments each replan for this stage
    private Boolean active;        // only active plans are eligible for generation
    private String status;         // ACTIVE / COMPLETED / SUPERSEDED
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

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
    }

    public String getMustAdvance() {
        return mustAdvance;
    }

    public void setMustAdvance(String mustAdvance) {
        this.mustAdvance = mustAdvance;
    }

    public String getMustNotDo() {
        return mustNotDo;
    }

    public void setMustNotDo(String mustNotDo) {
        this.mustNotDo = mustNotDo;
    }

    public String getStoryBeats() {
        return storyBeats;
    }

    public void setStoryBeats(String storyBeats) {
        this.storyBeats = storyBeats;
    }

    public String getEndingIntent() {
        return endingIntent;
    }

    public void setEndingIntent(String endingIntent) {
        this.endingIntent = endingIntent;
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
