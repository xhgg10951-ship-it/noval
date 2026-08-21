package com.example.storyai.memory.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.ExtractMemoryRequest;
import com.example.storyai.ai.dto.ExtractMemoryResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.common.exception.AiServiceException;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * Memory extraction orchestration (TASK-027/032, AT-G01..G04).
 *
 * <pre>
 * Build request -> Call Python Extractor (OUTSIDE tx) -> Validate
 * -> Persist candidates as PENDING
 * </pre>
 *
 * <p>Candidates are stored with their {@code suggestedAction} but a
 * {@code PENDING} processing status. The decision to apply them is made by
 * {@link CandidateProcessingService} (AUTO-safe-apply / REVIEW-queue / IGNORE).
 * The AI HTTP call is OUTSIDE any DB transaction.</p>
 */
@Service
public class MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

    private final StoryService storyService;
    private final MemoryService memoryService;
    private final AiServiceClient aiServiceClient;

    public MemoryExtractionService(StoryService storyService,
                                   MemoryService memoryService,
                                   AiServiceClient aiServiceClient) {
        this.storyService = storyService;
        this.memoryService = memoryService;
        this.aiServiceClient = aiServiceClient;
    }

    /** Extracts memory candidates from a freshly generated chapter. */
    public List<MemoryCandidate> extractForChapter(Chapter chapter) {
        Story story = storyService.getStory(chapter.getStoryId());
        List<StoryConstraint> constraints = storyService.getConstraints(chapter.getStoryId());

        List<PlanStageRequest.StateItem> existingState = memoryService.getCurrentState(story.getId()).stream()
                .map(s -> new PlanStageRequest.StateItem(s.getCategory(), s.getSubject(), s.getField(), s.getValue()))
                .toList();
        List<PlanStageRequest.ConstraintItem> constraintItems = constraints.stream()
                .map(c -> new PlanStageRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();

        ExtractMemoryRequest request = new ExtractMemoryRequest(
                chapter.getContent(),
                chapter.getSummary(),
                chapter.getChapterNumber(),
                existingState,
                constraintItems
        );

        ExtractMemoryResponse ai = aiServiceClient.extractMemory(request); // OUTSIDE tx
        validate(ai);

        List<MemoryCandidate> saved = ai.candidates().stream()
                .map(c -> toCandidate(story.getId(), chapter.getId(), c))
                .map(memoryService::saveCandidate)
                .toList();
        log.info("Extracted {} memory candidates from chapter {}", saved.size(), chapter.getId());
        return saved;
    }

    private MemoryCandidate toCandidate(Long storyId, Long chapterId, ExtractMemoryResponse.MemoryCandidate c) {
        MemoryCandidate m = new MemoryCandidate();
        m.setStoryId(storyId);
        m.setSourceChapterId(chapterId);
        m.setType(c.type());
        m.setSubject(c.subject());
        m.setField(c.field());
        m.setValue(c.value());
        m.setSuggestedAction(normalizeAction(c.suggestedAction()));
        m.setEvidence(c.evidence());
        m.setProcessingStatus("PENDING"); // decision deferred to processing service
        m.setApplied(false);
        return m;
    }

    private String normalizeAction(String action) {
        if (action == null) return "REVIEW";
        return switch (action.toUpperCase()) {
            case "AUTO", "REVIEW", "IGNORE" -> action.toUpperCase();
            default -> "REVIEW";
        };
    }

    private void validate(ExtractMemoryResponse ai) {
        if (ai == null || ai.candidates() == null) {
            throw new AiServiceException("AI 记忆提取服务返回空响应");
        }
    }
}
