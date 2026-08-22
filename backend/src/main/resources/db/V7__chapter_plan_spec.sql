-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V7 (TASK-114: ChapterPlan -> ChapterSpec)
-- Expand chapter_plan into a full ChapterSpec while keeping goal / expected_progress.
-- Lists (must_advance / must_not_do / story_beats) stored as JSON TEXT.
-- Additive; legacy rows stay valid (NULL fields).
-- NOTE: MySQL does not support ADD COLUMN IF NOT EXISTS; applied once per env.
-- ============================================================================

ALTER TABLE chapter_plan
    ADD COLUMN target_characters INT NULL
        COMMENT '本章目标字数（Phase 2 章节控制）';

ALTER TABLE chapter_plan
    ADD COLUMN must_advance TEXT NULL
        COMMENT '本章必须推进的要点（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN must_not_do TEXT NULL
        COMMENT '本章禁止做的事（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN story_beats TEXT NULL
        COMMENT '本章剧情节拍（JSON 数组）';

ALTER TABLE chapter_plan
    ADD COLUMN ending_intent TEXT NULL
        COMMENT '本章结尾意图';
