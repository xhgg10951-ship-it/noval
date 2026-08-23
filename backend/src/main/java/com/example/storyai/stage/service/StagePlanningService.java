package com.example.storyai.stage.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.chapter.service.GenerationJobService;
import com.example.storyai.common.exception.AiServiceException;
import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.context.StoryContextReader;
import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.model.Stage;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * Stage planning orchestration (TASK-015):
 *
 * <pre>
 * Load Story -> Build Planner Request -> Call Python -> Validate Response
 * -> Save Stage Plan (transactional, in StageService)
 * </pre>
 *
 * The AI HTTP call stays OUTSIDE any DB transaction; only persistence is
 * transactional.
 */
@Service
public class StagePlanningService {

    private static final Logger log = LoggerFactory.getLogger(StagePlanningService.class);

    private final StoryService storyService;
    private final StageService stageService;
    private final StoryContextReader contextReader;
    private final AiServiceClient aiServiceClient;
    // TASK-137: replan must respect in-flight generation jobs.
    private final GenerationJobService jobService;
    // TASK-139: replan prompt lists the stage's already-written beats.
    private final com.example.storyai.chapter.service.ChapterService chapterService;

    public StagePlanningService(StoryService storyService,
                                StageService stageService,
                                StoryContextReader contextReader,
                                AiServiceClient aiServiceClient,
                                GenerationJobService jobService,
                                com.example.storyai.chapter.service.ChapterService chapterService) {
        this.storyService = storyService;
        this.stageService = stageService;
        this.contextReader = contextReader;
        this.aiServiceClient = aiServiceClient;
        this.jobService = jobService;
        this.chapterService = chapterService;
    }

    /** Creates a new stage for the story and generates its initial plan (AT-B01). */
    public Stage createStagePlan(Long storyId, String direction, Integer targetChapterCount) {
        Story story = storyService.getStory(storyId);
        List<StoryConstraint> constraints = storyService.getConstraints(storyId);

        PlanStageRequest request = buildRequest(story, constraints, direction, targetChapterCount);
        PlanStageResponse plan = callPlanner(request, false);
        return stageService.saveNewStage(storyId, direction, targetChapterCount, plan);
    }

    /**
     * Full replan (AT-B02, TASK-016): the planner regenerates the complete plan
     * for the new target count — old plans are replaced, not truncated.
     *
     * <p>TASK-134 guard: this WHOLESALE path is only safe before generation starts
     * (no chapter references these plan rows yet). Once a stage is ACTIVE or
     * COMPLETED, deleting its plans would orphan completed Chapter↔Plan history,
     * so callers must use {@link #replanRemaining(Long, Integer, String)} instead.</p>
     */
    public Stage replanStage(Long stageId, Integer targetChapterCount) {
        Stage stage = stageService.getStage(stageId);
        if (!"PLANNING".equals(stage.getStatus())) {
            throw new IllegalStateException(
                    "只有 PLANNING 状态的阶段可以整体重规划；已激活的阶段请使用「重新规划剩余章节」，已完成的历史计划不可删除");
        }
        Story story = storyService.getStory(stage.getStoryId());
        List<StoryConstraint> constraints = storyService.getConstraints(stage.getStoryId());

        PlanStageRequest request = buildRequest(story, constraints, stage.getDirection(),
                targetChapterCount);
        PlanStageResponse plan = callPlanner(request, true);
        return stageService.replacePlans(stageId, targetChapterCount, plan);
    }

    /**
     * TASK-136 — Replan Remaining (v0.1.1 Phase 4). The author may change the
     * FUTURE of an ACTIVE/PAUSED stage without destroying history:
     *
     * <pre>
     * completed plans preserved
     * old remaining → SUPERSEDED
     * current Story State + continuation context → Planner
     * new-version remaining plans start at the next real story chapter number
     * </pre>
     *
     * @param stageId               target stage (must be ACTIVE or PAUSED)
     * @param remainingChapterCount size of the new remainder
     * @param authorInstruction     optional steering text for the Planner
     */
    public Stage replanRemaining(Long stageId, Integer remainingChapterCount,
                                 String authorInstruction) {
        Stage stage = stageService.getStage(stageId);
        if (!"ACTIVE".equals(stage.getStatus()) && !"PAUSED".equals(stage.getStatus())) {
            throw new IllegalStateException(
                    "只有 ACTIVE 或 PAUSED 状态的阶段可以重新规划剩余章节，当前状态: " + stage.getStatus());
        }
        // TASK-137: a RUNNING/PENDING generation must reach its checkpoint first.
        // The author pauses (or stops); replanning mid-flight would desync the
        // job's view of the plan queue. PAUSED jobs are allowed by design.
        com.example.storyai.chapter.model.GenerationJob latestJob =
                jobService.findLatestByStage(stageId);
        if (latestJob != null
                && ("RUNNING".equals(latestJob.getStatus())
                        || "PENDING".equals(latestJob.getStatus()))) {
            throw new IllegalStateException(
                    "当前阶段正在后台生成中，请先暂停或停止生成任务，到达安全检查点后再重新规划剩余章节");
        }
        Story story = storyService.getStory(stage.getStoryId());
        List<StoryConstraint> constraints = storyService.getConstraints(stage.getStoryId());

        // Optional author steering rides on the stage direction — no DTO contract
        // change needed; the Planner treats direction as the arc instruction.
        String direction = stage.getDirection();
        if (authorInstruction != null && !authorInstruction.isBlank()) {
            direction = direction + "\n作者对剩余章节的调整指示：" + authorInstruction.trim();
        }

        PlanStageRequest request = buildRequest(story, constraints, direction,
                remainingChapterCount);
        // TASK-139 fix: the Planner must know which BEATS of THIS stage are already
        // written, otherwise it re-plans from the stage start and duplicates them
        // (observed on AC-106: new plan #10 repeated the enrollment beat of ch1-2).
        // Rides on direction text — no DTO contract change.
        List<com.example.storyai.chapter.model.Chapter> doneChapters =
                chapterService.listByStage(stageId).stream()
                        .sorted(java.util.Comparator.comparing(
                                com.example.storyai.chapter.model.Chapter::getChapterNumber))
                        .toList();
        if (!doneChapters.isEmpty()) {
            java.util.Map<Long, String> goalByPlanId = stageService.getPlans(stageId).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ChapterPlan::getId, ChapterPlan::getGoal));
            List<String> beats = doneChapters.stream()
                    .map(c -> "- 第" + c.getChapterNumber() + "章（" + c.getTitle() + "）："
                            + (c.getPlanId() == null ? "（无计划）"
                                    : goalByPlanId.getOrDefault(c.getPlanId(), c.getTitle())))
                    .toList();
            direction = direction + "\n\n本阶段已完成章节的既成事实（新计划必须从这些事实之后继续，"
                    + "严禁重复或重演以下任何节拍）：\n" + String.join("\n", beats);
            request = buildRequest(story, constraints, direction, remainingChapterCount);
        }
        PlanStageResponse plan = callPlanner(request, true);

        // RH-03: planVersion separates historical rows, so logical chapter order
        // must not be shifted behind superseded history. Start the new remainder
        // at the next real story chapter number (4 after Chapters 1..3), while V1
        // superseded orders remain queryable as history.
        List<ChapterPlan> existing = stageService.getPlans(stageId);
        Integer currentChapterNumber = contextReader.getCurrentChapterNumber(story.getId());
        int baseOrder = currentChapterNumber == null ? 0 : currentChapterNumber;
        int newVersion = existing.stream()
                .mapToInt(p -> p.getPlanVersion() == null ? 1 : p.getPlanVersion())
                .max().orElse(1) + 1;
        PlanStageResponse shifted = shiftOrders(plan, baseOrder);

        return stageService.replanRemaining(stageId, remainingChapterCount, shifted, newVersion);
    }

    /** Re-numbers relative Planner items after the latest real chapter number. */
    private PlanStageResponse shiftOrders(PlanStageResponse plan, int baseOrder) {
        List<PlanStageResponse.ChapterPlanItem> shifted = new java.util.ArrayList<>();
        for (PlanStageResponse.ChapterPlanItem item : plan.chapterPlans()) {
            shifted.add(new PlanStageResponse.ChapterPlanItem(
                    item.order() + baseOrder,
                    item.goal(),
                    item.expectedProgress(),
                    item.targetCharacters(),
                    item.mustAdvance(),
                    item.mustNotDo(),
                    item.storyBeats(),
                    item.endingIntent()));
        }
        return new PlanStageResponse(plan.suggestedChapterCount(), shifted);
    }

    public Stage confirmPlan(Long stageId) {
        return stageService.confirmPlan(stageId);
    }

    public ChapterPlan updatePlanGoal(Long planId, String goal) {
        return stageService.updatePlanGoal(planId, goal);
    }

    // ---- helpers ----

    /**
     * v1 context assembly: core idea + constraints + stage direction (+ target
     * count for replanning). CurrentState / memories arrive in M4.
     */
    private PlanStageRequest buildRequest(Story story,
                                          List<StoryConstraint> constraints,
                                          String direction,
                                          Integer targetChapterCount) {
        List<PlanStageRequest.ConstraintItem> constraintItems = constraints.stream()
                .map(c -> new PlanStageRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();
        // TASK-106: wire existing story context instead of empty placeholders.
        // For a brand-new first stage these may legitimately be empty; for later
        // stages they now carry the real Current State / Story Memory / recent
        // chapter summaries so the Planner continues the existing story (RC-01).
        // TASK-107/108: add Planner continuation context v2 — relationships,
        // current chapter number, completed-stage summaries, recent chapter
        // summaries and a structured continuation anchor — so the Planner knows
        // it is continuing an existing story (fixes RC-01 continuation loop).
        Long storyId = story.getId();
        return new PlanStageRequest(
                story.getCoreIdea(),
                constraintItems,
                direction,
                contextReader.getCurrentStateItems(storyId),
                contextReader.getStoryMemoryItems(storyId),
                contextReader.getRecentContext(storyId, 3),
                targetChapterCount,
                contextReader.getPlannerRelationshipItems(storyId),
                contextReader.getCurrentChapterNumber(storyId),
                contextReader.getCompletedStageSummaries(storyId),
                contextReader.getRecentChapterSummaries(storyId, 3),
                contextReader.buildContinuationAnchor(storyId, 800),
                // v0.1.1 Phase 6 (TASK-154): long-form position (target/current/arc)
                contextReader.buildLongFormPosition(story.getId())
        );
    }

    private PlanStageResponse callPlanner(PlanStageRequest request, boolean replan) {
        PlanStageResponse plan = replan
                ? aiServiceClient.replanStage(request)
                : aiServiceClient.planStage(request);
        validatePlan(plan);
        return plan;
    }

    /**
     * Structured-response validation: the planner must return a positive count
     * and a contiguous, non-empty plan. Anything else is a contract violation.
     */
    private void validatePlan(PlanStageResponse plan) {
        if (plan == null) {
            throw new AiServiceException("AI 规划服务返回空响应");
        }
        if (plan.suggestedChapterCount() <= 0) {
            throw new AiServiceException("AI 规划服务返回的章节数无效: " + plan.suggestedChapterCount());
        }
        List<PlanStageResponse.ChapterPlanItem> items = plan.chapterPlans();
        if (items == null || items.isEmpty()) {
            throw new AiServiceException("AI 规划服务未返回任何章节计划");
        }
        if (items.size() != plan.suggestedChapterCount()) {
            log.warn("Planner count mismatch: suggested={}, plans={}",
                    plan.suggestedChapterCount(), items.size());
            throw new AiServiceException(String.format(
                    "AI 规划服务返回的章节数(%d)与计划数量(%d)不一致",
                    plan.suggestedChapterCount(), items.size()));
        }
        for (int i = 0; i < items.size(); i++) {
            PlanStageResponse.ChapterPlanItem item = items.get(i);
            if (item.goal() == null || item.goal().isBlank()) {
                throw new AiServiceException("第 " + (i + 1) + " 章的目标为空");
            }
            if (item.order() != i + 1) {
                throw new AiServiceException(String.format(
                        "章节序号不连续: 期望 %d, 实际 %d", i + 1, item.order()));
            }
        }
    }
}

