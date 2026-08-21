-- V5: GenerationJob persistence for multi-chapter generation (M5 / TASK-036)
-- Tracks a single generation run over a Stage in either STEP or CONTINUOUS mode.
-- One active job per stage is sufficient for v0.1 (no concurrent multi-job scope).

CREATE TABLE IF NOT EXISTS generation_job (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    stage_id           BIGINT        NOT NULL,
    mode               VARCHAR(16)   NOT NULL,            -- STEP / CONTINUOUS
    current_plan_index INT           NOT NULL DEFAULT 0,   -- plans completed so far
    total              INT           NOT NULL DEFAULT 0,   -- number of plans in the stage
    status             VARCHAR(16)   NOT NULL DEFAULT 'PENDING', -- PENDING/RUNNING/PAUSED/COMPLETED/FAILED
    phase              VARCHAR(16)   NULL,                 -- PLANNING/WRITING/MEMORY/CHECKPOINT
    last_error         TEXT          NULL,                -- captured failure detail
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_job_stage (stage_id),
    CONSTRAINT fk_job_stage FOREIGN KEY (stage_id) REFERENCES stage (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
