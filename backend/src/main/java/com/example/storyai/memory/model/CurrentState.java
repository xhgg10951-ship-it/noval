package com.example.storyai.memory.model;

import java.time.LocalDateTime;

/**
 * Current value of a single story-state slot (TASK-030, ARCHITECTURE §14-15).
 *
 * <p>Current Value First: only the latest value is stored per
 * (story, category, subject, field) slot. Historical changes remain discoverable
 * via the chapter / story_memory tables.</p>
 */
public class CurrentState {

    private Long id;
    private Long storyId;
    private String category;  // LOCATION / INVENTORY / PHYSICAL_CONDITION / EMOTION / CURRENT_GOAL
    private String subject;
    private String field;     // the specific slot name (e.g. 'location', 'weapon')
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
