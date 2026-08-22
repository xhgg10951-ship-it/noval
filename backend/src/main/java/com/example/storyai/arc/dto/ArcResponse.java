package com.example.storyai.arc.dto;

import com.example.storyai.arc.model.Arc;

/** Response DTO for an arc (v0.1.1 Phase 6 / TASK-152). */
public class ArcResponse {

    private Long id;
    private Long storyId;
    private String title;
    private String goal;
    private Integer targetStartChapter;
    private Integer targetEndChapter;
    private String status;
    private String createdAt;
    private String updatedAt;

    public ArcResponse() {
    }

    public ArcResponse(Arc a) {
        this.id = a.getId();
        this.storyId = a.getStoryId();
        this.title = a.getTitle();
        this.goal = a.getGoal();
        this.targetStartChapter = a.getTargetStartChapter();
        this.targetEndChapter = a.getTargetEndChapter();
        this.status = a.getStatus();
        this.createdAt = a.getCreatedAt() == null ? null : a.getCreatedAt().toString();
        this.updatedAt = a.getUpdatedAt() == null ? null : a.getUpdatedAt().toString();
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public Integer getTargetStartChapter() {
        return targetStartChapter;
    }

    public void setTargetStartChapter(Integer targetStartChapter) {
        this.targetStartChapter = targetStartChapter;
    }

    public Integer getTargetEndChapter() {
        return targetEndChapter;
    }

    public void setTargetEndChapter(Integer targetEndChapter) {
        this.targetEndChapter = targetEndChapter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
