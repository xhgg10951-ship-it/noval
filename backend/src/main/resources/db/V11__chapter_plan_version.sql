-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V11 (TASK-133: plan versioning)
-- Additive version/active/status columns for Replan Remaining. Legacy plans
-- become version 1, active, ACTIVE so existing behavior is preserved.
-- ============================================================================

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS plan_version INT NOT NULL DEFAULT 1
        COMMENT 'Replan iteration for the stage; increments each replan';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'Only active plans are eligible for generation';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE / COMPLETED / SUPERSEDED';
