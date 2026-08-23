package com.example.storyai.stage.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.model.Stage;

/**
 * Response DTO for a Stage with its current chapter plans.
 */
public class StageResponse {

    private Long id;
    private Long storyId;
    private String direction;
    private String status;
    private Integer suggestedChapterCount;
    private Integer targetChapterCount;
    private Integer targetCharacters;
    private List<ChapterPlanResponse> plans;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StageResponse() {
    }

    public StageResponse(Stage stage, List<ChapterPlan> plans) {
        this.id = stage.getId();
        this.storyId = stage.getStoryId();
        this.direction = stage.getDirection();
        this.status = stage.getStatus();
        this.suggestedChapterCount = stage.getSuggestedChapterCount();
        this.targetChapterCount = stage.getTargetChapterCount();
        this.targetCharacters = stage.getTargetCharacters();
        this.plans = plans.stream().map(ChapterPlanResponse::new).collect(Collectors.toList());
        this.createdAt = stage.getCreatedAt();
        this.updatedAt = stage.getUpdatedAt();
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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSuggestedChapterCount() {
        return suggestedChapterCount;
    }

    public void setSuggestedChapterCount(Integer suggestedChapterCount) {
        this.suggestedChapterCount = suggestedChapterCount;
    }

    public Integer getTargetChapterCount() {
        return targetChapterCount;
    }

    public void setTargetChapterCount(Integer targetChapterCount) {
        this.targetChapterCount = targetChapterCount;
    }

    public Integer getTargetCharacters() {
        return targetCharacters;
    }

    public void setTargetCharacters(Integer targetCharacters) {
        this.targetCharacters = targetCharacters;
    }

    public List<ChapterPlanResponse> getPlans() {
        return plans;
    }

    public void setPlans(List<ChapterPlanResponse> plans) {
        this.plans = plans;
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
