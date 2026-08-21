package com.example.storyai.memory.model;

import java.time.LocalDateTime;

/**
 * A single character-relationship description (TASK-031, ARCHITECTURE §16).
 *
 * <p>Natural-language description is preferred over a numeric affinity score.
 * One row per (story, subjectA, subjectB) pair; updated in place.</p>
 */
public class RelationshipState {

    private Long id;
    private Long storyId;
    private String subjectA;
    private String subjectB;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    public String getSubjectA() { return subjectA; }
    public void setSubjectA(String subjectA) { this.subjectA = subjectA; }

    public String getSubjectB() { return subjectB; }
    public void setSubjectB(String subjectB) { this.subjectB = subjectB; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
