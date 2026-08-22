package com.example.storyai.chapter.model;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of one chapter's content (v0.1.1 Phase 5 / TASK-140).
 *
 * <p>Every write to a chapter (AI generation, manual edit, AI rewrite/polish)
 * creates a NEW revision row; the chapter row only points at the revision whose
 * content it currently exposes via {@code current_revision_id}. Revisions are
 * never mutated or deleted — history is the product.</p>
 */
public class ChapterRevision {

    /** How this revision came into existence. */
    public static final String SOURCE_AI_GENERATED = "AI_GENERATED";
    public static final String SOURCE_MANUAL_EDIT = "MANUAL_EDIT";
    public static final String SOURCE_AI_REWRITE = "AI_REWRITE";
    public static final String SOURCE_AI_POLISH = "AI_POLISH";

    private Long id;
    private Long chapterId;
    private Integer versionNumber;
    private String content;
    private String sourceType;
    private LocalDateTime createdAt;

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
