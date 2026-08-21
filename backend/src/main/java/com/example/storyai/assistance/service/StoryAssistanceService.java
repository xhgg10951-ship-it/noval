package com.example.storyai.assistance.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.StoryQueryRequest;
import com.example.storyai.ai.dto.StoryQueryResponse;
import com.example.storyai.ai.dto.SuggestDirectionsRequest;
import com.example.storyai.ai.dto.SuggestDirectionsResponse;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.chapter.service.ChapterService;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;
import com.example.storyai.memory.service.MemoryService;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * Author-assistance orchestration (M6 / TASK-042..046).
 *
 * <p>Two read-only capabilities that help the author without touching the story:
 * <ul>
 *   <li>{@code suggestDirections} — propose &ge;3 distinct next-stage directions
 *       (AT-K01/K02); the author may pick / edit-as-new-direction / reject all,
 *       and nothing is applied automatically (AT-K03).</li>
 *   <li>{@code queryStory} — answer a natural-language question from the story's
 *       structured Current State / Relationships / Story Memories (AT-J01..J05).
 *       No RAG is added (M6 scope) — only already-persisted info is passed.</li>
 * </ul>
 *
 * <p>Both build their request from the story + memory, then call the Python AI
 * Service OUTSIDE any DB transaction (same rule as planning/generation).</p>
 */
@Service
public class StoryAssistanceService {

    private static final Logger log = LoggerFactory.getLogger(StoryAssistanceService.class);

    private final StoryService storyService;
    private final MemoryService memoryService;
    private final ChapterService chapterService;
    private final AiServiceClient aiServiceClient;

    public StoryAssistanceService(StoryService storyService,
                                  MemoryService memoryService,
                                  ChapterService chapterService,
                                  AiServiceClient aiServiceClient) {
        this.storyService = storyService;
        this.memoryService = memoryService;
        this.chapterService = chapterService;
        this.aiServiceClient = aiServiceClient;
    }

    /** Proposes distinct next-story directions. Read-only; does not modify the story. */
    public SuggestDirectionsResponse suggestDirections(Long storyId) {
        Story story = storyService.getStory(storyId);
        SuggestDirectionsRequest request = buildSuggestionRequest(story, storyService.getConstraints(storyId));
        return aiServiceClient.suggestDirections(request); // OUTSIDE tx
    }

    /** Answers a question about the story from its structured memory. */
    public StoryQueryResponse queryStory(Long storyId, String question) {
        Story story = storyService.getStory(storyId);
        StoryQueryRequest request = buildQueryRequest(story, storyService.getConstraints(storyId), question);
        return aiServiceClient.storyQuery(request); // OUTSIDE tx
    }

    // ---- request assembly (shared context) ----

    private SuggestDirectionsRequest buildSuggestionRequest(Story story, List<StoryConstraint> constraints) {
        return new SuggestDirectionsRequest(
                story.getCoreIdea(),
                toConstraintItems(constraints),
                toStateItems(memoryService.getCurrentState(story.getId())),
                toMemoryItems(memoryService.getStoryMemories(story.getId())),
                toRelationshipItems(memoryService.getRelationships(story.getId())),
                recentContext(story.getId())
        );
    }

    private StoryQueryRequest buildQueryRequest(Story story, List<StoryConstraint> constraints, String question) {
        return new StoryQueryRequest(
                question,
                toStateItems(memoryService.getCurrentState(story.getId())),
                toMemoryItems(memoryService.getStoryMemories(story.getId())),
                toRelationshipItems(memoryService.getRelationships(story.getId())),
                recentContext(story.getId()),
                toMemoryItems(memoryService.getStoryMemories(story.getId()))
        );
    }

    /** Most recent chapter summary as lightweight continuity context. */
    private String recentContext(Long storyId) {
        return chapterService.listByStory(storyId).stream()
                .max(Comparator.comparing(Chapter::getChapterNumber))
                .map(Chapter::getSummary)
                .filter(Objects::nonNull)
                .orElse("");
    }

    // ---- DTO mappers (match the Python contract field shapes) ----

    private List<PlanStageRequest.ConstraintItem> toConstraintItems(List<StoryConstraint> constraints) {
        if (constraints == null) return List.of();
        return constraints.stream()
                .map(c -> new PlanStageRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();
    }

    private List<PlanStageRequest.StateItem> toStateItems(List<CurrentState> states) {
        if (states == null) return List.of();
        return states.stream()
                .map(s -> new PlanStageRequest.StateItem(s.getCategory(), s.getSubject(), s.getField(), s.getValue()))
                .toList();
    }

    private List<PlanStageRequest.MemoryItem> toMemoryItems(List<StoryMemory> memories) {
        if (memories == null) return List.of();
        return memories.stream()
                .map(m -> new PlanStageRequest.MemoryItem(m.getType(), m.getSubject(), m.getDescription()))
                .toList();
    }

    private List<GenerateChapterRequest.RelationshipItem> toRelationshipItems(List<RelationshipState> rels) {
        if (rels == null) return List.of();
        return rels.stream()
                .map(r -> new GenerateChapterRequest.RelationshipItem(r.getSubjectA(), r.getSubjectB(), r.getDescription()))
                .toList();
    }
}
