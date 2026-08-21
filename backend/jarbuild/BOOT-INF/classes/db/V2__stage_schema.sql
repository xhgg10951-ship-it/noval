-- ============================================================================
-- AI Story Co-Author v0.1 — Schema V2 (TASK-011: minimum Stage Planning schema)
-- Design rule (TASKS.md): only Stage + ChapterPlan now — the planning vertical
-- slice. Chapter / Memory / GenerationJob tables come in later milestones.
-- NO branch system, NO plan version history: a replan replaces the current
-- chapter_plan rows of the stage inside one transaction (v0.1 keeps exactly one
-- live plan per stage). Idempotent: safe to re-run.
-- ============================================================================

-- Stage: one planning + generation phase of a story, driven by an author
-- direction. Status lifecycle (v0.1): PLANNING -> ACTIVE -> COMPLETED | ABANDONED.
CREATE TABLE IF NOT EXISTS stage (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    story_id                BIGINT       NOT NULL,
    direction               TEXT         NOT NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PLANNING',
    suggested_chapter_count INT          NULL,
    target_chapter_count    INT          NULL,
    created_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_stage_story_id (story_id),
    KEY idx_stage_status (status),
    CONSTRAINT fk_stage_story
        FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ChapterPlan: one planned chapter within a stage. `chapter_order` is the
-- planner-assigned sequence (1..N). `goal` is author-editable (AT-B03).
CREATE TABLE IF NOT EXISTS chapter_plan (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    stage_id         BIGINT       NOT NULL,
    chapter_order    INT          NOT NULL,
    goal             TEXT         NOT NULL,
    expected_progress VARCHAR(500) NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_plan_stage_id (stage_id),
    KEY idx_plan_order (stage_id, chapter_order),
    CONSTRAINT fk_plan_stage
        FOREIGN KEY (stage_id) REFERENCES stage (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
