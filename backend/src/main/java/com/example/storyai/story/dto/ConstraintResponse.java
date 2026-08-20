package com.example.storyai.story.dto;

import com.example.storyai.story.model.StoryConstraint;

/**
 * Response DTO for a single story constraint.
 */
public class ConstraintResponse {

    private Long id;
    private String type;
    private String content;
    private Integer sortOrder;

    public ConstraintResponse() {
    }

    public ConstraintResponse(StoryConstraint c) {
        this.id = c.getId();
        this.type = c.getType();
        this.content = c.getContent();
        this.sortOrder = c.getSortOrder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
