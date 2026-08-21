package com.example.storyai.stage.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.stage.dto.ChapterPlanResponse;
import com.example.storyai.stage.dto.CreateStageRequest;
import com.example.storyai.stage.dto.ReplanRequest;
import com.example.storyai.stage.dto.StageResponse;
import com.example.storyai.stage.dto.StageSummary;
import com.example.storyai.stage.dto.UpdatePlanGoalRequest;
import com.example.storyai.stage.service.StagePlanningService;
import com.example.storyai.stage.service.StageService;

/**
 * REST API for Stage planning (TASK-015/016, AT-B01..B03).
 *
 * <pre>
 * POST /api/stories/{storyId}/stages   -> create stage + initial plan (201)
 * GET  /api/stories/{storyId}/stages   -> list stage summaries (200)
 * GET  /api/stages/{stageId}           -> stage with plans (200 / 404)
 * POST /api/stages/{stageId}/replan    -> full replan w/ target count (200)
 * POST /api/stages/{stageId}/confirm   -> PLANNING -> ACTIVE (200)
 * PUT  /api/stages/plans/{planId}      -> edit chapter goal (200 / 404)
 * </pre>
 */
@RestController
public class StageController {

    private final StagePlanningService planningService;
    private final StageService stageService;

    public StageController(StagePlanningService planningService, StageService stageService) {
        this.planningService = planningService;
        this.stageService = stageService;
    }

    @PostMapping("/api/stories/{storyId}/stages")
    public ResponseEntity<StageResponse> createStage(@PathVariable Long storyId,
                                                     @Valid @RequestBody CreateStageRequest request) {
        var stage = planningService.createStagePlan(
                storyId, request.getDirection(), request.getTargetChapterCount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(stage, stageService.getPlans(stage.getId())));
    }

    @GetMapping("/api/stories/{storyId}/stages")
    public List<StageSummary> listStages(@PathVariable Long storyId) {
        return stageService.listStages(storyId).stream()
                .map(StageSummary::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/stages/{stageId}")
    public StageResponse getStage(@PathVariable Long stageId) {
        var stage = stageService.getStage(stageId);
        return toResponse(stage, stageService.getPlans(stageId));
    }

    @PostMapping("/api/stages/{stageId}/replan")
    public StageResponse replan(@PathVariable Long stageId,
                                @Valid @RequestBody ReplanRequest request) {
        var stage = planningService.replanStage(stageId, request.getTargetChapterCount());
        return toResponse(stage, stageService.getPlans(stageId));
    }

    @PostMapping("/api/stages/{stageId}/confirm")
    public StageResponse confirm(@PathVariable Long stageId) {
        var stage = planningService.confirmPlan(stageId);
        return toResponse(stage, stageService.getPlans(stageId));
    }

    @PutMapping("/api/stages/plans/{planId}")
    public ChapterPlanResponse updatePlanGoal(@PathVariable Long planId,
                                              @Valid @RequestBody UpdatePlanGoalRequest request) {
        return new ChapterPlanResponse(planningService.updatePlanGoal(planId, request.getGoal()));
    }

    // ---- mapping helper ----

    private StageResponse toResponse(com.example.storyai.stage.model.Stage stage,
                                     List<com.example.storyai.stage.model.ChapterPlan> plans) {
        return new StageResponse(stage, plans);
    }
}
