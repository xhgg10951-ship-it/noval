package com.example.storyai.memory.model;

import java.time.LocalDateTime;

/**
 * A memory candidate extracted from a chapter by the Python Memory Extractor
 * (TASK-025/027). Not yet fully applied — its fate is decided by processing
 * rules (AUTO / REVIEW / IGNORE), see {@link ProcessingStatus}.
 */
public class MemoryCandidate {

    private Long id;
    private Long storyId;
    private Long sourceChapterId;
    private String type;          // CURRENT_STATE / RELATIONSHIP / STORY_MEMORY / DETAIL ...
    private String subject;
    private String field;
    private String value;
    private String suggestedAction; // AUTO / REVIEW / IGNORE
    private String evidence;
    private String processingStatus = "PENDING"; // PENDING / APPLIED / IGNORED
    private boolean applied = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    public Long getSourceChapterId() { return sourceChapterId; }
    public void setSourceChapterId(Long sourceChapterId) { this.sourceChapterId = sourceChapterId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
