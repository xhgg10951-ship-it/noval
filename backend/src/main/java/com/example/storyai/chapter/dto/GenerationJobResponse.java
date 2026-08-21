package com.example.storyai.chapter.dto;

import java.time.LocalDateTime;

import com.example.storyai.chapter.model.GenerationJob;

/**
 * Response DTO for a generation job (M5 / TASK-041).
 *
 * <p>Exposes enough to drive the progress UI: current/total chapters, mode, status,
 * current phase, and the last error (when FAILED).</p>
 */
public class GenerationJobResponse {

    private Long id;
    private Long stageId;
    private String mode;
    private int currentPlanIndex;
    private int total;
    private String status;
    private String phase;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GenerationJobResponse() {
    }

    public GenerationJobResponse(GenerationJob j) {
        this.id = j.getId();
        this.stageId = j.getStageId();
        this.mode = j.getMode();
        this.currentPlanIndex = j.getCurrentPlanIndex();
        this.total = j.getTotal();
        this.status = j.getStatus();
        this.phase = j.getPhase();
        this.lastError = j.getLastError();
        this.createdAt = j.getCreatedAt();
        this.updatedAt = j.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getCurrentPlanIndex() {
        return currentPlanIndex;
    }

    public void setCurrentPlanIndex(int currentPlanIndex) {
        this.currentPlanIndex = currentPlanIndex;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
