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
    // ---- v0.1.1 Phase 5 (TASK-140): revision + approval lifecycle ----
    private Long currentRevisionId;
    private Integer currentRevisionVersion;
    private String sourceType; // source of the current revision
    private String status; // DRAFT / APPROVED
    private String memoryExtractionStatus; // PENDING / COMPLETED / FAILED / STALE
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
        this.currentRevisionId = c.getCurrentRevisionId();
        this.status = c.getStatus();
        this.memoryExtractionStatus = c.getMemoryExtractionStatus();
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

    public Long getCurrentRevisionId() {
        return currentRevisionId;
    }

    public void setCurrentRevisionId(Long currentRevisionId) {
        this.currentRevisionId = currentRevisionId;
    }

    public Integer getCurrentRevisionVersion() {
        return currentRevisionVersion;
    }

    public void setCurrentRevisionVersion(Integer currentRevisionVersion) {
        this.currentRevisionVersion = currentRevisionVersion;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMemoryExtractionStatus() {
        return memoryExtractionStatus;
    }

    public void setMemoryExtractionStatus(String memoryExtractionStatus) {
        this.memoryExtractionStatus = memoryExtractionStatus;
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
