package com.example.storyai.chapter.model;

import java.time.LocalDateTime;

/**
 * Generated chapter persisted for a Story (M3 / TASK-018).
 *
 * <p>One chapter per {@code ChapterPlan} (enforced by uk_chapter_plan). Full prose
 * is stored in {@code content}; {@code summary} is a short recap used later as
 * generation context for the next chapter.</p>
 */
public class Chapter {

    private Long id;
    private Long storyId;
    private Long stageId;
    private Long planId;
    private int chapterNumber;
    private String title;
    private String content;
    private String summary;
    private String generationStatus; // DRAFT / GENERATED / FAILED
    // ---- TASK-119: chapter length measurement ----
    private Integer targetCharacters;
    private Integer actualCharacterCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
    }

    public Integer getActualCharacterCount() {
        return actualCharacterCount;
    }

    public void setActualCharacterCount(Integer actualCharacterCount) {
        this.actualCharacterCount = actualCharacterCount;
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
