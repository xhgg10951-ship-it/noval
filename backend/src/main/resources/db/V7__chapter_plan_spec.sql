-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V7 (TASK-114: ChapterPlan -> ChapterSpec)
-- Expand chapter_plan into a full ChapterSpec while keeping goal / expected_progress.
-- Lists (must_advance / must_not_do / story_beats) stored as JSON TEXT.
-- Additive + idempotent; legacy rows stay valid (NULL fields).
-- ============================================================================

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS target_characters INT NULL
        COMMENT '本章目标字数（Phase 2 章节控制）';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS must_advance TEXT NULL
        COMMENT '本章必须推进的要点（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS must_not_do TEXT NULL
        COMMENT '本章禁止做的事（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS story_beats TEXT NULL
        COMMENT '本章剧情节拍（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN IF NOT EXISTS ending_intent TEXT NULL
        COMMENT '本章结尾意图';
