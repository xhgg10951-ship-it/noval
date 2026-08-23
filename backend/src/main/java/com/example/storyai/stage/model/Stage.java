package com.example.storyai.stage.model;

/**
 * Stage entity — one planning + generation phase of a story (TASK-012).
 * Status lifecycle v0.1: PLANNING -> ACTIVE -> COMPLETED | ABANDONED.
 */
public class Stage {

    private Long id;
    private Long storyId;
    private String direction;
    private String status;
    private Integer suggestedChapterCount;
    private Integer targetChapterCount;
    private Integer targetCharacters;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSuggestedChapterCount() {
        return suggestedChapterCount;
    }

    public void setSuggestedChapterCount(Integer suggestedChapterCount) {
        this.suggestedChapterCount = suggestedChapterCount;
    }

    public Integer getTargetChapterCount() {
        return targetChapterCount;
    }

    public void setTargetChapterCount(Integer targetChapterCount) {
        this.targetChapterCount = targetChapterCount;
    }

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
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
