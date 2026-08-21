-- V4: Memory vertical slice schema (M4 / TASK-025)
-- memory_candidate, current_state, relationship_state, story_memory
-- Idempotent: CREATE TABLE IF NOT EXISTS so re-running migrations is safe.

CREATE TABLE IF NOT EXISTS memory_candidate (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id        BIGINT       NOT NULL,
    source_chapter_id BIGINT     NULL,
    type            VARCHAR(32)  NOT NULL,           -- CURRENT_STATE / RELATIONSHIP / STORY_MEMORY / DETAIL ...
    subject         VARCHAR(128) NOT NULL,
    field           VARCHAR(64)  NULL,
    value           TEXT         NOT NULL,
    suggested_action VARCHAR(16) NOT NULL,           -- AUTO / REVIEW / IGNORE
    evidence        TEXT         NULL,
    processing_status VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING / APPLIED / IGNORED
    applied         BIT(1)      NOT NULL DEFAULT 0,  -- 1 once it has been applied to live memory/state
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidate_story   FOREIGN KEY (story_id)         REFERENCES story (id)         ON DELETE CASCADE,
    CONSTRAINT fk_candidate_chapter FOREIGN KEY (source_chapter_id) REFERENCES chapter (id)       ON DELETE SET NULL,
    INDEX idx_candidate_story (story_id),
    INDEX idx_candidate_status (story_id, processing_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS current_state (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id    BIGINT       NOT NULL,
    category    VARCHAR(32)  NOT NULL,   -- LOCATION / INVENTORY / PHYSICAL_CONDITION / EMOTION / CURRENT_GOAL ...
    subject     VARCHAR(128) NOT NULL,
    field       VARCHAR(64)  NOT NULL,   -- the specific slot (e.g. 'location', 'weapon')
    value       TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_state_story FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE,
    UNIQUE KEY uk_state_slot (story_id, category, subject, field),
    INDEX idx_state_story (story_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS relationship_state (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id    BIGINT       NOT NULL,
    subject_a   VARCHAR(128) NOT NULL,
    subject_b   VARCHAR(128) NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rel_story FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rel_pair (story_id, subject_a, subject_b),
    INDEX idx_rel_story (story_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS story_memory (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id        BIGINT       NOT NULL,
    type            VARCHAR(32)  NOT NULL,   -- EVENT / DETAIL / FORESHADOW / SECRET / PROMISE / ANOMALY ...
    subject         VARCHAR(128) NULL,
    description     TEXT         NOT NULL,
    source_chapter_id BIGINT     NULL,
    evidence        TEXT         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_memory_story   FOREIGN KEY (story_id)         REFERENCES story (id)     ON DELETE CASCADE,
    CONSTRAINT fk_memory_chapter FOREIGN KEY (source_chapter_id) REFERENCES chapter (id)  ON DELETE SET NULL,
    INDEX idx_memory_story (story_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
