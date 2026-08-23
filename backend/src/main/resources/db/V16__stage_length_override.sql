-- v0.1.1 frozen length hierarchy: Chapter override > Stage override > Story default.
-- Additive and nullable so existing v0.1/v0.1.1 data inherits its Story default.
ALTER TABLE stage
    ADD COLUMN target_characters INT NULL
        COMMENT '阶段默认章节字数；单章 ChapterSpec 可覆盖';
