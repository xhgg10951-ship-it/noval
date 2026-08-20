package com.example.storyai.story.dto;

import com.example.storyai.story.model.Story;

import java.time.LocalDateTime;

/**
 * Lightweight summary for the list endpoint (no constraints/content).
 */
public class StorySummary {

    private Long id;
    private String name;
    private LocalDateTime createdAt;

    public StorySummary() {
    }

    public StorySummary(Story story) {
        this.id = story.getId();
        this.name = story.getName();
        this.createdAt = story.getCreatedAt();
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
