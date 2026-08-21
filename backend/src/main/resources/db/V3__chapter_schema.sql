-- V3: Chapter schema for single-chapter generation (M3 / TASK-018)
-- One generated chapter per ChapterPlan. Full body is stored in `content`.

CREATE TABLE IF NOT EXISTS chapter (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    story_id          BIGINT        NOT NULL,
    stage_id          BIGINT        NOT NULL,
    plan_id           BIGINT        NULL,
    chapter_number    INT           NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    content           MEDIUMTEXT    NOT NULL,
    summary           TEXT          NULL,
    generation_status VARCHAR(32)   NOT NULL DEFAULT 'GENERATED',
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_chapter_story_number (story_id, chapter_number),
    UNIQUE KEY uk_chapter_plan (plan_id),
    CONSTRAINT fk_chapter_story FOREIGN KEY (story_id) REFERENCES story (id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_stage FOREIGN KEY (stage_id) REFERENCES stage (id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_plan FOREIGN KEY (plan_id) REFERENCES chapter_plan (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
