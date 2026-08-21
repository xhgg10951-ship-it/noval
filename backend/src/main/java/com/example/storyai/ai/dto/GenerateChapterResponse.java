package com.example.storyai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured response from the Python writer (TASK-020).
 *
 * <pre>
 * title   : chapter title
 * content : full prose body
 * summary : short recap reused as context for the next chapter
 * </pre>
 *
 * <p>Shape MUST match the Pydantic {@code GenerateChapterResponse} in
 * {@code ai-service/app/schemas/models.py}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateChapterResponse(
        String title,
        String content,
        String summary
) {
}
