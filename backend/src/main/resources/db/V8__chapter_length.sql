-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V8 (TASK-119: chapter length measurement)
-- Additive columns for chapter length control (Phase 2). Legacy rows stay valid.
-- ============================================================================

ALTER TABLE chapter
    ADD COLUMN IF NOT EXISTS target_characters INT NULL
        COMMENT '本章目标字数（来自 ChapterSpec / Story 默认）';

ALTER TABLE chapter
    ADD COLUMN IF NOT EXISTS actual_character_count INT NULL
        COMMENT '本章实际字数（生成后按 CJK 计数规则计算）';
