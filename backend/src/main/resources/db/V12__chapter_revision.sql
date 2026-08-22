-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V12 (TASK-140/141: ChapterRevision)
-- New chapter_revision table + chapter.current_revision_id/status.
-- Backfills every legacy chapter with an AI_GENERATED revision #1 so v0.1
-- content stays readable and becomes the first immutable revision (TASK-141:
-- "不得丢已有正文"). Additive; applied once per environment (MySQL has no
-- ADD COLUMN IF NOT EXISTS).
-- ============================================================================

CREATE TABLE IF NOT EXISTS chapter_revision (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    chapter_id     BIGINT        NOT NULL,
    version_number INT           NOT NULL,
    content        MEDIUMTEXT    NOT NULL,
    source_type    VARCHAR(32)   NOT NULL DEFAULT 'AI_GENERATED'
        COMMENT 'AI_GENERATED / MANUAL_EDIT / AI_REWRITE / AI_POLISH',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_revision_chapter_version (chapter_id, version_number),
    CONSTRAINT fk_revision_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE chapter
    ADD COLUMN current_revision_id BIGINT NULL
        COMMENT 'Revision whose content the chapter currently exposes';

ALTER TABLE chapter
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT until the author approves; APPROVED afterwards';

-- TASK-141 backfill: one AI_GENERATED revision per legacy chapter, then point
-- current_revision_id at it. Guarded WHERE clauses keep re-runs harmless.
INSERT INTO chapter_revision (chapter_id, version_number, content, source_type)
SELECT c.id, 1, c.content, 'AI_GENERATED'
FROM chapter c
LEFT JOIN chapter_revision r ON r.chapter_id = c.id AND r.version_number = 1
WHERE r.id IS NULL;

UPDATE chapter c
JOIN chapter_revision r ON r.chapter_id = c.id AND r.version_number = 1
SET c.current_revision_id = r.id
WHERE c.current_revision_id IS NULL;
