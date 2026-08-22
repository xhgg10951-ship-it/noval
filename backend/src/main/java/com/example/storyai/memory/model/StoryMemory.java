package com.example.storyai.memory.model;

import java.time.LocalDateTime;

/**
 * A piece of long-term story memory the system or author decided to keep
 * (TASK-029/033, ARCHITECTURE §18). Feeds Writer context, Planner and Story Query.
 */
public class StoryMemory {

    private Long id;
    private Long storyId;
    private String type;     // v0.1.1 Phase 7 enum (TASK-158): see MemoryTypes
    private String subject;
    private String description;
    private Long sourceChapterId;
    private String evidence;
    // ---- v0.1.1 Phase 7 / TASK-157: Memory v2 ----
    private int importance = 3;   // 1..5
    private String scope = "STORY"; // CHAPTER / STAGE / ARC / STORY
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSourceChapterId() { return sourceChapterId; }
    public void setSourceChapterId(Long sourceChapterId) { this.sourceChapterId = sourceChapterId; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
