package com.example.storyai.chapter.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/chapters/{chapterId}/content}
 * (v0.1.1 Phase 5 — Manual Edit, TASK-142).
 */
public class EditChapterContentRequest {

    @NotBlank(message = "正文不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
