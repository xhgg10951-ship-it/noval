package com.example.storyai.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured response from the Python Memory Extractor (TASK-027).
 *
 * <p>Shape MUST match the Pydantic {@code ExtractMemoryResponse} in
 * {@code ai-service/app/schemas/models.py}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractMemoryResponse(
        List<MemoryCandidate> candidates
) {

    /** One extracted candidate. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MemoryCandidate(
            String type,
            String subject,
            String field,
            String value,
            String suggestedAction,
            String evidence,
            // ---- v0.1.1 Phase 7 (TASK-159): Memory v2 contract ----
            Integer importance, // 1..5, validated/clamped in Java
            String scope        // CHAPTER/STAGE/ARC/STORY, validated in Java
    ) {
    }
}
