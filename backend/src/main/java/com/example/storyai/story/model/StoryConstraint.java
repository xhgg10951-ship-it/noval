package com.example.storyai.story.model;

import java.time.LocalDateTime;

/**
 * Database model for the {@code story_constraint} table (ARCHITECTURE §9).
 *
 * <p>A long-lived author rule such as STYLE / PERSPECTIVE / WORLD_RULE. v0.1 has
 * no complex hierarchy — just type + content.
 */
public class StoryConstraint {

    private Long id;
    private Long storyId;
    private String type;
    private String content;
    private Integer sortOrder;
    private LocalDateTime createdAt;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
