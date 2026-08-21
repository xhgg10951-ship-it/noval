package com.example.storyai.chapter.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.common.exception.AiServiceException;
import com.example.storyai.common.exception.NoPendingChapterException;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.service.CandidateProcessingService;
import com.example.storyai.memory.service.MemoryExtractionService;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;
import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.model.Stage;
import com.example.storyai.stage.service.StageService;

/**
 * Single-chapter generation orchestration (TASK-022/023, AT-C01):
 *
 * <pre>
 * Load Context -> Call Python Writer (outside tx) -> Validate Response
 * -> Persist Chapter
 * </pre>
 *
 * <p>The AI HTTP call stays OUTSIDE any DB transaction; only persistence runs
 * transactionally (in {@link ChapterService}). One chapter is produced per
 * {@code ChapterPlan} (enforced by the uk_chapter_plan unique key); the next
 * pending plan is chosen by lowest chapter order.</p>
 */
@Service
public class ChapterGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ChapterGenerationService.class);

    private final StoryService storyService;
    private final StageService stageService;
    private final ChapterService chapterService;
    private final AiServiceClient aiServiceClient;
    private final MemoryExtractionService extractionService;
    private final CandidateProcessingService processingService;

    public ChapterGenerationService(StoryService storyService,
                                    StageService stageService,
                                    ChapterService chapterService,
                                    AiServiceClient aiServiceClient,
                                    MemoryExtractionService extractionService,
                                    CandidateProcessingService processingService) {
        this.storyService = storyService;
        this.stageService = stageService;
        this.chapterService = chapterService;
        this.aiServiceClient = aiServiceClient;
        this.extractionService = extractionService;
        this.processingService = processingService;
    }

    /** Generates the next pending chapter for a stage (AT-C01). */
    public Chapter generateNextChapter(Long stageId) {
        Stage stage = stageService.getStage(stageId);
        Story story = storyService.getStory(stage.getStoryId());
        List<StoryConstraint> constraints = storyService.getConstraints(stage.getStoryId());
        List<ChapterPlan> plans = stageService.getPlans(stageId);

        Set<Long> generatedPlanIds = chapterService.listByStage(stageId).stream()
                .map(Chapter::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        ChapterPlan nextPlan = plans.stream()
                .filter(p -> !generatedPlanIds.contains(p.getId()))
                .min(Comparator.comparing(ChapterPlan::getChapterOrder))
                .orElseThrow(() -> new NoPendingChapterException(stageId));

        // Continuity: summary of the most recent chapter generated for the story so far.
        String prevSummary = chapterService.listByStory(story.getId()).stream()
                .max(Comparator.comparing(Chapter::getChapterNumber))
                .map(Chapter::getSummary)
                .orElse(null);

        GenerateChapterRequest request = buildRequest(story, constraints, stage, nextPlan, prevSummary);

        GenerateChapterResponse ai = aiServiceClient.generateChapter(request); // OUTSIDE tx
        validate(ai);

        Chapter chapter = new Chapter();
        chapter.setStoryId(story.getId());
        chapter.setStageId(stage.getId());
        chapter.setPlanId(nextPlan.getId());
        chapter.setChapterNumber(chapterService.nextChapterNumber(story.getId()));
        chapter.setTitle(ai.title());
        chapter.setContent(ai.content());
        chapter.setSummary(ai.summary());
        chapter.setGenerationStatus("GENERATED");

        Chapter saved = chapterService.saveChapter(chapter);

        // M4 (TASK-032): Save Chapter -> Extract Memory -> Save Candidates -> Apply AUTO -> Checkpoint.
        // Extraction calls Python OUTSIDE the generation tx; candidate persistence + AUTO apply run
        // in their own transactions inside the memory services.
        List<MemoryCandidate> candidates = extractionService.extractForChapter(saved);
        for (MemoryCandidate c : candidates) {
            processingService.autoProcess(c); // AUTO -> apply, REVIEW -> pending, IGNORE -> ignored
        }
        return saved;
    }

    /**
     * v1 context assembly (TASK-022): story constraints + stage direction +
     * the plan's chapter goal + previous chapter summary as recent context.
     * CurrentState / StoryMemory / RelationshipState arrive in M4 and are left
     * empty here (allowed by the contract).
     */
    private GenerateChapterRequest buildRequest(Story story,
                                                List<StoryConstraint> constraints,
                                                Stage stage,
                                                ChapterPlan plan,
                                                String prevSummary) {
        List<GenerateChapterRequest.ConstraintItem> constraintItems = constraints.stream()
                .map(c -> new GenerateChapterRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();
        return new GenerateChapterRequest(
                story.getCoreIdea(),
                constraintItems,
                stage.getDirection(),
                plan.getGoal(),
                plan.getChapterOrder(),
                List.of(),
                List.of(),
                List.of(),
                prevSummary == null ? "" : prevSummary
        );
    }

    /** Structured-response validation: title/content/summary must all be present. */
    private void validate(GenerateChapterResponse ai) {
        if (ai == null) {
            throw new AiServiceException("AI 写作服务返回空响应");
        }
        if (isBlank(ai.title()) || isBlank(ai.content()) || isBlank(ai.summary())) {
            throw new AiServiceException("AI 写作服务返回内容不完整（标题/正文/摘要缺失）");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
