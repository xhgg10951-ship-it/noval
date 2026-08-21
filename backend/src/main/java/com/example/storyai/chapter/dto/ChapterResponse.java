package com.example.storyai.chapter.dto;

import java.time.LocalDateTime;

import com.example.storyai.chapter.model.Chapter;

/**
 * Response DTO for a generated chapter (TASK-024).
 */
public class ChapterResponse {

    private Long id;
    private Long storyId;
    private Long stageId;
    private Long planId;
    private int chapterNumber;
    private String title;
    private String content;
    private String summary;
    private String generationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChapterResponse() {
    }

    public ChapterResponse(Chapter c) {
        this.id = c.getId();
        this.storyId = c.getStoryId();
        this.stageId = c.getStageId();
        this.planId = c.getPlanId();
        this.chapterNumber = c.getChapterNumber();
        this.title = c.getTitle();
        this.content = c.getContent();
        this.summary = c.getSummary();
        this.generationStatus = c.getGenerationStatus();
        this.createdAt = c.getCreatedAt();
        this.updatedAt = c.getUpdatedAt();
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

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(String generationStatus) {
        this.generationStatus = generationStatus;
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
