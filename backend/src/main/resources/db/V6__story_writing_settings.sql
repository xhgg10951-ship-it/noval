-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V6 (TASK-113: Story writing settings)
-- Additive migration: introduces per-story writing settings used by Phase 2
-- (Chapter Control). Old rows remain valid — defaults keep behaviour unchanged.
-- NOTE: MySQL does not support ADD COLUMN IF NOT EXISTS (MariaDB-only syntax);
-- this migration is applied once per environment, like a Flyway version.
-- MySQL 8.4 / utf8mb4.
-- ============================================================================

-- default_target_characters: target length for generated chapters (Phase 2).
-- Default 3000 keeps the v0.1.1 contract default; NULL allowed for legacy rows.
ALTER TABLE story
    ADD COLUMN default_target_characters INT NULL DEFAULT 3000
        COMMENT '单章目标字数（Phase 2 章节控制默认 3000）';

-- writing_style: free-text authoring style hint surfaced to the Writer (Phase 8).
ALTER TABLE story
    ADD COLUMN writing_style VARCHAR(200) NULL
        COMMENT '作者设定的写作风格提示（Phase 8 文风）';

-- target_chapter_count: long-form target (Phase 6 Pace). Column added now
-- (additive, NULL for legacy), but Pace behaviour is NOT implemented until Phase 6.
ALTER TABLE story
    ADD COLUMN target_chapter_count INT NULL
        COMMENT '长篇目标章节数（Phase 6 Pace；本阶段仅建列，不实现节奏逻辑）';
