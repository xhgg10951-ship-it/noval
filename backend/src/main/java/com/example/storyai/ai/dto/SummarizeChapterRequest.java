package com.example.storyai.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request for {@code POST /ai/summarize-chapter} (RH-01 / HB-001).
 *
 * <p>The summary must be derived only from the current chapter body. Keeping
 * this contract content-only prevents an obsolete summary from feeding itself
 * back into the refresh operation.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SummarizeChapterRequest(String content) {
}
