package com.example.storyai.story.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.story.dto.ConstraintInput;
import com.example.storyai.story.dto.ConstraintResponse;
import com.example.storyai.story.dto.CreateStoryRequest;
import com.example.storyai.story.dto.StoryResponse;
import com.example.storyai.story.dto.StorySummary;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * REST API for the Story domain (TASK-009, AT-A01).
 *
 * <pre>
 * POST   /api/stories       -> create story (201)
 * GET    /api/stories/{id}  -> get story (200 / 404)
 * GET    /api/stories       -> list summaries (200)
 * </pre>
 *
 * Controller only does HTTP + DTO mapping; all persistence/decisions go through
 * {@link StoryService} (ARCHITECTURE §23/§24).
 */
@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    public ResponseEntity<StoryResponse> create(@Valid @RequestBody CreateStoryRequest request) {
        Story story = new Story();
        story.setName(request.getName());
        story.setCoreIdea(request.getCoreIdea());
        story.setInitialStageDirection(request.getInitialStageDirection());
        story.setDefaultTargetCharacters(request.getDefaultTargetCharacters());
        List<StoryConstraint> constraints = toConstraints(request.getConstraints());

        Story created = storyService.createStory(story, constraints);
        // Re-read so the response reflects DB-generated state (status default,
        // created_at/updated_at timestamps) rather than in-memory nulls.
        Story persisted = storyService.getStory(created.getId());
        List<StoryConstraint> saved = storyService.getConstraints(created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(persisted, saved));
    }

    @GetMapping("/{id}")
    public StoryResponse get(@PathVariable Long id) {
        Story story = storyService.getStory(id);
        List<StoryConstraint> constraints = storyService.getConstraints(id);
        return toResponse(story, constraints);
    }

    @GetMapping
    public List<StorySummary> list() {
        return storyService.listStories().stream()
                .map(StorySummary::new)
                .collect(Collectors.toList());
    }

    // ---- mapping helpers ----

    private List<StoryConstraint> toConstraints(List<ConstraintInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream().map(input -> {
            StoryConstraint c = new StoryConstraint();
            c.setType(input.getType());
            c.setContent(input.getContent());
            c.setSortOrder(input.getSortOrder());
            return c;
        }).collect(Collectors.toList());
    }

    private StoryResponse toResponse(Story story, List<StoryConstraint> constraints) {
        StoryResponse response = new StoryResponse();
        response.setId(story.getId());
        response.setName(story.getName());
        response.setCoreIdea(story.getCoreIdea());
        response.setInitialStageDirection(story.getInitialStageDirection());
        response.setDefaultTargetCharacters(story.getDefaultTargetCharacters());
        response.setStatus(story.getStatus());
        response.setConstraints(constraints.stream()
                .map(ConstraintResponse::new)
                .collect(Collectors.toList()));
        response.setCreatedAt(story.getCreatedAt());
        response.setUpdatedAt(story.getUpdatedAt());
        return response;
    }
}
