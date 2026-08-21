package com.example.storyai.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the Python Story Query endpoint (M6 / TASK-045).
 *
 * <p>Shape MUST match Pydantic {@code StoryQueryResponse} — a single natural
 * language answer. The Python side returns an explicit "未知" style answer when
 * the story has no determined information (AT-J05).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryQueryResponse(
        @JsonProperty("answer") String answer
) {
}
