package com.example.storyai.stage.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.chapter.mapper.ChapterMapper;
import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.stage.mapper.ChapterPlanMapper;
import com.example.storyai.stage.mapper.StageMapper;
import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.model.Stage;

/**
 * Transactional persistence for Stage + ChapterPlan (TASK-012).
 *
 * <p>Deliberately separate from {@link StagePlanningService}: the AI HTTP call
 * happens OUTSIDE any DB transaction; only these methods run transactionally.</p>
 */
@Service
public class StageService {

    private final StageMapper stageMapper;
    private final ChapterPlanMapper chapterPlanMapper;
    private final ChapterMapper chapterMapper;

    public StageService(StageMapper stageMapper, ChapterPlanMapper chapterPlanMapper,
                        ChapterMapper chapterMapper) {
        this.stageMapper = stageMapper;
        this.chapterPlanMapper = chapterPlanMapper;
        this.chapterMapper = chapterMapper;
    }

    /**
     * Inserts a new PLANNING stage and its chapter plans in one transaction.
     * Returns the persisted stage (id populated).
     */
    @Transactional
    public Stage saveNewStage(Long storyId, String direction, Integer targetChapterCount,
                              PlanStageResponse plan) {
        Stage stage = new Stage();
        stage.setStoryId(storyId);
        stage.setDirection(direction);
        stage.setStatus("PLANNING");
        stage.setSuggestedChapterCount(plan.suggestedChapterCount());
        stage.setTargetChapterCount(targetChapterCount);
        stageMapper.insert(stage);
        insertPlans(stage.getId(), plan, 1);
        return stageMapper.findById(stage.getId());
    }

    /**
     * Full replan (AT-B02): replaces the stage's chapter plans and updates the
     * counts in one transaction. The stage keeps its id and direction.
     */
    @Transactional
    public Stage replacePlans(Long stageId, Integer targetChapterCount, PlanStageResponse plan) {
        Stage stage = requireStage(stageId);
        chapterPlanMapper.deleteByStageId(stageId);
        stageMapper.updatePlanCounts(stageId, plan.suggestedChapterCount(), targetChapterCount);
        insertPlans(stageId, plan, 1);
        return stageMapper.findById(stageId);
    }

    /** PLANNING -> ACTIVE. The plan is now the confirmed basis for generation. */
    @Transactional
    public Stage confirmPlan(Long stageId) {
        Stage stage = requireStage(stageId);
        if (!"PLANNING".equals(stage.getStatus())) {
            throw new IllegalStateException("只有 PLANNING 状态的阶段可以确认，当前状态: " + stage.getStatus());
        }
        stageMapper.updateStatus(stageId, "ACTIVE");
        return stageMapper.findById(stageId);
    }

    /** AT-B03: author edits a chapter goal. */
    @Transactional
    public ChapterPlan updatePlanGoal(Long planId, String goal) {
        ChapterPlan plan = chapterPlanMapper.findById(planId);
        if (plan == null) {
            throw new ResourceNotFoundException("ChapterPlan", planId);
        }
        chapterPlanMapper.updateGoal(planId, goal);
        return chapterPlanMapper.findById(planId);
    }

    /**
     * TASK-131 — Stage completion lifecycle. Transitions an ACTIVE stage to
     * COMPLETED once all its plans are generated AND every generated chapter's
     * memory extraction has reached a stable state (COMPLETED; FAILED/STALE are
     * left for the author to reconcile rather than silently completing). The
     * caller (orchestration) only invokes this from the job COMPLETED path, so a
     * Job cannot become COMPLETED while the Stage lifecycle is inconsistent.
     */
    @Transactional
    public Stage completeStage(Long stageId) {
        Stage stage = requireStage(stageId);
        if (!"ACTIVE".equals(stage.getStatus())) {
            return stage; // idempotent: only ACTIVE -> COMPLETED
        }
        boolean allExtracted = chapterMapper.findByStageId(stageId).stream()
                .allMatch(c -> com.example.storyai.chapter.model.MemoryExtractionStatus.COMPLETED
                        .equals(c.getMemoryExtractionStatus()));
        if (!allExtracted) {
            return stage; // do not flip to COMPLETED while a chapter memory is unstable
        }
        stageMapper.updateStatus(stageId, "COMPLETED");
        return stageMapper.findById(stageId);
    }

    public Stage getStage(Long stageId) {
        return requireStage(stageId);
    }

    public List<ChapterPlan> getPlans(Long stageId) {
        requireStage(stageId);
        return chapterPlanMapper.findByStageId(stageId);
    }

    /**
     * TASK-135 — the generation queue. Returns only plans that are still active and
     * not completed, so superseded (replanned-away) plans are never regenerated.
     */
    public List<ChapterPlan> getActiveRemainingPlans(Long stageId) {
        requireStage(stageId);
        return chapterPlanMapper.findActiveRemaining(stageId);
    }

    /** TASK-134/135: a plan whose chapter has been generated is done. */
    public void markPlanCompleted(Long planId) {
        chapterPlanMapper.markCompleted(planId);
    }

    public List<Stage> listStages(Long storyId) {
        return stageMapper.findByStoryId(storyId);
    }

    // ---- helpers ----

    private Stage requireStage(Long stageId) {
        Stage stage = stageMapper.findById(stageId);
        if (stage == null) {
            throw new ResourceNotFoundException("Stage", stageId);
        }
        return stage;
    }

    private void insertPlans(Long stageId, PlanStageResponse plan, int planVersion) {
        List<ChapterPlan> rows = plan.chapterPlans().stream().map(item -> {
            ChapterPlan p = new ChapterPlan();
            p.setStageId(stageId);
            p.setChapterOrder(item.order());
            p.setGoal(item.goal());
            p.setExpectedProgress(item.expectedProgress());
            // TASK-116: persist the full ChapterSpec returned by the Planner so
            // no planner-provided field is silently dropped before MySQL.
            p.setTargetCharacters(item.targetCharacters());
            p.setMustAdvance(item.mustAdvance());
            p.setMustNotDo(item.mustNotDo());
            p.setStoryBeats(item.storyBeats());
            p.setEndingIntent(item.endingIntent());
            // TASK-133: every plan row carries its version + active/status.
            p.setPlanVersion(planVersion);
            p.setActive(true);
            p.setStatus("ACTIVE");
            return p;
        }).toList();
        if (!rows.isEmpty()) {
            chapterPlanMapper.insertBatch(rows);
        }
    }

    /**
     * TASK-136 — Replan Remaining (the safe mid-generation replan).
     *
     * <p>Completed plans (those with a generated chapter) are NEVER deleted; only
     * the still-active, non-completed plans are superseded. New-version active
     * plans are then created from the fresh Planner response. This preserves the
     * author's history ("change the future, not the past") instead of the old
     * {@code deleteByStageId} wholesale replace.</p>
     *
     * @param stageId            target stage
     * @param targetChapterCount optional override
     * @param plan               fresh Planner response for the REMAINING arc
     * @param newVersion         monotonically increasing version for this stage
     */
    @Transactional
    public Stage replanRemaining(Long stageId, Integer targetChapterCount,
                                 PlanStageResponse plan, int newVersion) {
        requireStage(stageId);
        // TASK-134: supersede remaining (active, ACTIVE) plans — keep completed ones.
        chapterPlanMapper.supersedeRemaining(stageId);
        if (targetChapterCount != null) {
            stageMapper.updatePlanCounts(stageId, plan.suggestedChapterCount(), targetChapterCount);
        } else {
            stageMapper.updatePlanCounts(stageId, plan.suggestedChapterCount(), null);
        }
        insertPlans(stageId, plan, newVersion);
        return stageMapper.findById(stageId);
    }
}
