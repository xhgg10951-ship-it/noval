package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the Python Planner Suggestion endpoint (M6 / TASK-042).
 *
 * <p>Shape MUST match Pydantic {@code SuggestDirectionsResponse} — a list of
 * distinct direction proposals. The Java side never auto-applies these; the
 * author picks, edits, or rejects (AT-K03).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuggestDirectionsResponse(
        @JsonProperty("directions") List<DirectionItem> directions
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DirectionItem(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description
    ) {
    }
}
