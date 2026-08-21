package com.example.storyai.stage.dto;

import java.time.LocalDateTime;

import com.example.storyai.stage.model.Stage;

/**
 * Lightweight stage summary for list endpoints (no plans).
 */
public class StageSummary {

    private Long id;
    private Long storyId;
    private String status;
    private Integer suggestedChapterCount;
    private Integer targetChapterCount;
    private LocalDateTime createdAt;

    public StageSummary() {
    }

    public StageSummary(Stage stage) {
        this.id = stage.getId();
        this.storyId = stage.getStoryId();
        this.status = stage.getStatus();
        this.suggestedChapterCount = stage.getSuggestedChapterCount();
        this.targetChapterCount = stage.getTargetChapterCount();
        this.createdAt = stage.getCreatedAt();
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
