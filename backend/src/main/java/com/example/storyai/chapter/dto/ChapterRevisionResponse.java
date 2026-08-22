package com.example.storyai.chapter.dto;

import java.time.LocalDateTime;

import com.example.storyai.chapter.model.ChapterRevision;

/** Response DTO for one chapter revision (v0.1.1 Phase 5 / TASK-145). */
public class ChapterRevisionResponse {

    private Long id;
    private Long chapterId;
    private Integer versionNumber;
    private String content;
    private String sourceType;
    private LocalDateTime createdAt;

    public ChapterRevisionResponse() {
    }

    public ChapterRevisionResponse(ChapterRevision r) {
        this.id = r.getId();
        this.chapterId = r.getChapterId();
        this.versionNumber = r.getVersionNumber();
        this.content = r.getContent();
        this.sourceType = r.getSourceType();
        this.createdAt = r.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
