-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V14 (TASK-157: Memory v2 columns)
-- importance / scope / active on story_memory and memory_candidate.
-- Legacy rows: importance=3 (MEDIUM), scope='STORY', active=1 — readable and
-- treated as long-lived facts, matching v0.1 semantics. Additive; applied once.
-- ============================================================================

ALTER TABLE story_memory
    ADD COLUMN importance TINYINT NOT NULL DEFAULT 3
        COMMENT '1..5 (5 = must always reach the writer)';

ALTER TABLE story_memory
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'STORY'
        COMMENT 'CHAPTER / STAGE / ARC / STORY';

ALTER TABLE story_memory
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1
        COMMENT '0 when superseded/deduped; inactive rows never reach the writer';

ALTER TABLE memory_candidate
    ADD COLUMN importance TINYINT NOT NULL DEFAULT 3
        COMMENT '1..5 from the extractor (validated)';

ALTER TABLE memory_candidate
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'STORY'
        COMMENT 'CHAPTER / STAGE / ARC / STORY from the extractor (validated)';

CREATE INDEX idx_memory_active ON story_memory (story_id, active);
