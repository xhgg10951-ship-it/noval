-- ============================================================================
-- AI Story Co-Author v0.1 — Schema V1 (TASK-007: minimum Story schema)
-- Design rule (TASKS.md): only Story + StoryConstraint now. Stage / ChapterPlan /
-- Chapter / Memory tables are added in later milestones — do NOT add them here.
-- All tables target the `story_ai` database (utf8mb4).
-- Idempotent: safe to re-run.
-- ============================================================================

-- Story: a single novel project. Top-level scope for all child objects.
CREATE TABLE IF NOT EXISTS story (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(200) NOT NULL,
    core_idea              TEXT         NOT NULL,
    initial_stage_direction TEXT         NULL,
    status                  VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_story_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- StoryConstraint: long-lived author rules (STYLE / PERSPECTIVE / WORLD_RULE / ...).
CREATE TABLE IF NOT EXISTS story_constraint (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    story_id   BIGINT      NOT NULL,
    type       VARCHAR(64) NOT NULL,
    content    VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_constraint_story_id (story_id),
    CONSTRAINT fk_constraint_story
        FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
