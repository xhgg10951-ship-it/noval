package com.example.storyai.chapter.model;

import java.time.LocalDateTime;

/**
 * Tracks one multi-chapter generation run over a Stage (M5 / TASK-036).
 *
 * <p>A job is created when the author starts generation for a stage in either
 * {@code STEP} (checkpoint after every chapter, user must Continue) or
 * {@code CONTINUOUS} (run to completion or failure) mode. The same single-chapter
 * flow (ChapterGenerationService.generateNextChapter) is reused for every step;
 * only the orchestration (loop vs. checkpoint) differs.</p>
 *
 * <p>Status lifecycle: PENDING -> RUNNING -> (PAUSED | COMPLETED | FAILED).
 * PAUSED only applies to STEP mode; COMPLETED/FAILED terminate the job.
 * lastError is populated whenever status becomes FAILED.</p>
 */
public class GenerationJob {

    /** Generation modes. */
    public enum Mode {
        STEP,
        CONTINUOUS
    }

    /** Job status. */
    public enum Status {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED
    }

    /** Current processing phase (telemetry for the progress UI). */
    public enum Phase {
        PLANNING,
        WRITING,
        MEMORY,
        CHECKPOINT
    }

    private Long id;
    private Long stageId;
    private String mode;
    private int currentPlanIndex;
    private int total;
    private String status;
    private String phase;
    private String lastError;
    // TASK-129/130: runtime control signals (set by author via API, read by loop).
    private Boolean pauseRequested;
    private Boolean stopRequested;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getPauseRequested() {
        return pauseRequested;
    }

    public void setPauseRequested(Boolean pauseRequested) {
        this.pauseRequested = pauseRequested;
    }

    public Boolean getStopRequested() {
        return stopRequested;
    }

    public void setStopRequested(Boolean stopRequested) {
        this.stopRequested = stopRequested;
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
