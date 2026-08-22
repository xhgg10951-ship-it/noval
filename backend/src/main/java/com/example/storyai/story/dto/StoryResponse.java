package com.example.storyai.story.dto;

import java.util.List;

/**
 * Response DTO for a Story (with its constraints).
 */
public class StoryResponse {

    private Long id;
    private String name;
    private String coreIdea;
    private String initialStageDirection;
    private Integer defaultTargetCharacters;
    private String status;
    private List<ConstraintResponse> constraints;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

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

    public Integer getDefaultTargetCharacters() {
        return defaultTargetCharacters;
    }

    public void setDefaultTargetCharacters(Integer defaultTargetCharacters) {
        this.defaultTargetCharacters = defaultTargetCharacters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ConstraintResponse> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ConstraintResponse> constraints) {
        this.constraints = constraints;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
