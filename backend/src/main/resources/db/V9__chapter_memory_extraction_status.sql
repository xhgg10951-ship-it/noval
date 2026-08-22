-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V9 (TASK-123: chapter memory extraction status)
-- Additive column making the extraction state machine explicit. Legacy rows had
-- extraction run in v0.1, so the backward-compatible default is COMPLETED
-- (do NOT retroactively flag old chapters as PENDING/STALE).
-- NOTE: MySQL does not support ADD COLUMN IF NOT EXISTS; applied once per env.
-- ============================================================================

ALTER TABLE chapter
    ADD COLUMN memory_extraction_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        COMMENT 'Memory extraction lifecycle: PENDING / COMPLETED / FAILED / STALE';
