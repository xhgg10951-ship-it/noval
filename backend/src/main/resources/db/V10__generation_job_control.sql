-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V10 (TASK-129/130: pause/stop control)
-- Additive runtime control signals for background generation jobs.
-- NOTE: MySQL does not support ADD COLUMN IF NOT EXISTS; applied once per env.
-- ============================================================================

ALTER TABLE generation_job
    ADD COLUMN pause_requested TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Author requested PAUSE; loop stops at next checkpoint';

ALTER TABLE generation_job
    ADD COLUMN stop_requested TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Author requested STOP; loop terminates, chapters retained';
