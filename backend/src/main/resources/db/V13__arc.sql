-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V13 (TASK-151/152: Arc)
-- Long-form story arcs: coarse narrative segments with a chapter range and a
-- goal, consumed by the Planner as the "Current Arc" context (Phase 6).
-- Additive; applied once per environment (MySQL has no ADD COLUMN IF NOT EXISTS).
-- ============================================================================

CREATE TABLE IF NOT EXISTS arc (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    story_id             BIGINT        NOT NULL,
    title                VARCHAR(200)  NOT NULL,
    goal                 TEXT          NULL,
    target_start_chapter INT           NOT NULL,
    target_end_chapter   INT           NOT NULL,
    status               VARCHAR(16)   NOT NULL DEFAULT 'PLANNED'
        COMMENT 'PLANNED / ACTIVE / COMPLETED',
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_arc_story (story_id),
    CONSTRAINT fk_arc_story FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE,
    CONSTRAINT chk_arc_range CHECK (target_end_chapter >= target_start_chapter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
