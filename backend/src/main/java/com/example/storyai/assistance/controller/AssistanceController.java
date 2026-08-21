package com.example.storyai.assistance.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.ai.dto.StoryQueryResponse;
import com.example.storyai.ai.dto.SuggestDirectionsResponse;
import com.example.storyai.assistance.service.StoryAssistanceService;

/**
 * REST API for author assistance (M6 / TASK-042..046, AT-K01..K03 / AT-J01..J05).
 *
 * <pre>
 * POST /api/stories/{storyId}/suggest-directions  -> >=3 distinct next directions (read-only)
 * POST /api/stories/{storyId}/story-query          -> answer a natural-language question
 * </pre>
 */
@RestController
@RequestMapping("/api/stories")
public class AssistanceController {

    private final StoryAssistanceService assistanceService;

    public AssistanceController(StoryAssistanceService assistanceService) {
        this.assistanceService = assistanceService;
    }

    @PostMapping("/{storyId}/suggest-directions")
    public SuggestDirectionsResponse suggestDirections(@PathVariable Long storyId) {
        return assistanceService.suggestDirections(storyId);
    }

    @PostMapping("/{storyId}/story-query")
    public StoryQueryResponse queryStory(@PathVariable Long storyId,
                                          @RequestBody StoryQueryRequestBody body) {
        return assistanceService.queryStory(storyId, body.question());
    }

    /** Minimal request body for story query (just the question text). */
    public record StoryQueryRequestBody(String question) {
    }
}
