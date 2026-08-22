-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V10 (TASK-129/130: pause/stop control)
-- Additive runtime control signals for background generation jobs.
-- ============================================================================

ALTER TABLE generation_job
    ADD COLUMN IF NOT EXISTS pause_requested TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Author requested PAUSE; loop stops at next checkpoint';

ALTER TABLE generation_job
    ADD COLUMN IF NOT EXISTS stop_requested TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Author requested STOP; loop terminates, chapters retained';
