package com.example.storyai.stage.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.common.exception.AiServiceException;
import com.example.storyai.common.exception.ResourceNotFoundException;
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
    private final AiServiceClient aiServiceClient;

    public StagePlanningService(StoryService storyService,
                                StageService stageService,
                                AiServiceClient aiServiceClient) {
        this.storyService = storyService;
        this.stageService = stageService;
        this.aiServiceClient = aiServiceClient;
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
     */
    public Stage replanStage(Long stageId, Integer targetChapterCount) {
        Stage stage = stageService.getStage(stageId);
        Story story = storyService.getStory(stage.getStoryId());
        List<StoryConstraint> constraints = storyService.getConstraints(stage.getStoryId());

        PlanStageRequest request = buildRequest(story, constraints, stage.getDirection(),
                targetChapterCount);
        PlanStageResponse plan = callPlanner(request, true);
        return stageService.replacePlans(stageId, targetChapterCount, plan);
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
        return new PlanStageRequest(
                story.getCoreIdea(),
                constraintItems,
                direction,
                List.of(),
                List.of(),
                "",
                targetChapterCount
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
