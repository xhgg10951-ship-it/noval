package com.example.storyai.arc.model;

import java.time.LocalDateTime;

/**
 * A coarse narrative segment of a long-form story (v0.1.1 Phase 6 / TASK-151).
 *
 * <p>An Arc groups chapters into a range with a goal, giving the Planner the
 * "Current Arc" context: where the story is within the long-form structure and
 * what this segment is supposed to accomplish.</p>
 */
public class Arc {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private Long id;
    private Long storyId;
    private String title;
    private String goal;
    private Integer targetStartChapter;
    private Integer targetEndChapter;
    private String status; // PLANNED / ACTIVE / COMPLETED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
