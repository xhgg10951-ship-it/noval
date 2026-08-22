package com.example.storyai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Response for {@code POST /ai/polish-chapter} (v0.1.1 Phase 8 / TASK-167). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolishChapterResponse(
        String polishedContent
) {
}
