package com.example.storyai.story.model;

import java.time.LocalDateTime;

/**
 * Database model for the {@code story} table (ARCHITECTURE §8).
 *
 * <p>Story is the top-level scope for all child objects (constraints, stages,
 * chapters, memory). Column→field mapping relies on
 * {@code map-underscore-to-camel-case} (application.yml).
 */
public class Story {

    private Long id;
    private String name;
    private String coreIdea;
    private String initialStageDirection;
    private String status;
    private Integer defaultTargetCharacters;
    private String writingStyle;
    private Integer targetChapterCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoreIdea() {
        return coreIdea;
    }

    public void setCoreIdea(String coreIdea) {
        this.coreIdea = coreIdea;
    }

    public String getInitialStageDirection() {
        return initialStageDirection;
    }

    public void setInitialStageDirection(String initialStageDirection) {
        this.initialStageDirection = initialStageDirection;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDefaultTargetCharacters() {
        return defaultTargetCharacters;
    }

    public void setDefaultTargetCharacters(Integer defaultTargetCharacters) {
        this.defaultTargetCharacters = defaultTargetCharacters;
    }

    public String getWritingStyle() {
        return writingStyle;
    }

    public void setWritingStyle(String writingStyle) {
        this.writingStyle = writingStyle;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
