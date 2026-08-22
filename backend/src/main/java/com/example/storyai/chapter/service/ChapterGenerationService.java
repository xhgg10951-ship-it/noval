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
import com.example.storyai.context.StoryContextReader;
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
    private final StoryContextReader contextReader;
    private final AiServiceClient aiServiceClient;
    private final MemoryExtractionService extractionService;
    private final CandidateProcessingService processingService;

    public ChapterGenerationService(StoryService storyService,
                                    StageService stageService,
                                    ChapterService chapterService,
                                    StoryContextReader contextReader,
                                    AiServiceClient aiServiceClient,
                                    MemoryExtractionService extractionService,
                                    CandidateProcessingService processingService) {
        this.storyService = storyService;
        this.stageService = stageService;
        this.chapterService = chapterService;
        this.contextReader = contextReader;
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

        // TASK-111: continuity = last 2-3 chapter summaries + latest chapter ending
        // excerpt, so a single summary is no longer the only continuity anchor.
        String recentContext = contextReader.getRecentContextWithEnding(story.getId(), 3, 800);

        GenerateChapterRequest request = buildRequest(story, constraints, stage, nextPlan, recentContext);

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
        // TASK-119: record target (from ChapterSpec) + actual length (CJK count).
        chapter.setTargetCharacters(nextPlan.getTargetCharacters());
        chapter.setActualCharacterCount(
                com.example.storyai.common.util.TextLengthUtil.countCharacters(ai.content()));
        // TASK-124: the chapter is persisted with extraction PENDING; only set to
        // COMPLETED after a successful extraction pass. This makes the recovery
        // hole (TASK-126) detectable: a persisted-but-FAILED chapter is retried,
        // never silently advanced.
        chapter.setMemoryExtractionStatus(
                com.example.storyai.chapter.model.MemoryExtractionStatus.PENDING);

        Chapter saved = chapterService.saveChapter(chapter);

        // M4 (TASK-032): Save Chapter -> Extract Memory -> Save Candidates -> Apply AUTO -> Checkpoint.
        // Extraction calls Python OUTSIDE the generation tx; candidate persistence + AUTO apply run
        // in their own transactions inside the memory services.
        // TASK-124: any extraction failure marks the chapter FAILED but leaves the
        // chapter intact, so the Next Safe Action Resolver (TASK-125) retries the
        // SAME chapter instead of skipping to the next plan.
        try {
            List<MemoryCandidate> candidates = extractionService.extractForChapter(saved);
            for (MemoryCandidate c : candidates) {
                processingService.autoProcess(c); // AUTO -> apply, REVIEW -> pending, IGNORE -> ignored
            }
            saved.setMemoryExtractionStatus(
                    com.example.storyai.chapter.model.MemoryExtractionStatus.COMPLETED);
            chapterService.saveChapter(saved); // checkpoint the COMPLETED status
        } catch (Exception ex) {
            log.warn("Memory extraction failed for chapter {}: {}", saved.getId(), ex.getMessage());
            saved.setMemoryExtractionStatus(
                    com.example.storyai.chapter.model.MemoryExtractionStatus.FAILED);
            chapterService.saveChapter(saved); // persist FAILED so recovery can retry
            // Re-throw so the caller (job/continuous runner) sees the failure and
            // pauses safely rather than treating the chapter as fully done.
            throw ex;
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
                                                String recentContext) {
        List<GenerateChapterRequest.ConstraintItem> constraintItems = constraints.stream()
                .map(c -> new GenerateChapterRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();
        // TASK-110: wire existing story context instead of empty placeholders.
        // For the very first chapter these may legitimately be empty; afterwards
        // they carry the real Current State / Story Memory / Relationships so the
        // Writer actually uses them (fixes RC-02).
        // TASK-111: recentContext is now last 2-3 chapter summaries + latest
        // chapter ending excerpt (see generateNextChapter).
        // TASK-117: pass the FULL ChapterSpec from the plan (targetCharacters /
        // mustAdvance / mustNotDo / storyBeats / endingIntent) so the Writer
        // executes the plan, not just the goal (fixes RC-04 plan->writer drift).
        Long storyId = story.getId();
        return new GenerateChapterRequest(
                story.getCoreIdea(),
                constraintItems,
                stage.getDirection(),
                plan.getGoal(),
                plan.getChapterOrder(),
                contextReader.getWriterStateItems(storyId),
                contextReader.getWriterMemoryItems(storyId),
                contextReader.getRelationshipItems(storyId),
                recentContext == null ? "" : recentContext,
                plan.getTargetCharacters(),
                plan.getMustAdvance(),
                plan.getMustNotDo(),
                plan.getStoryBeats(),
                plan.getEndingIntent()
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
