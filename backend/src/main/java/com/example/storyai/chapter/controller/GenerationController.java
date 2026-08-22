package com.example.storyai.chapter.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.chapter.dto.GenerationJobResponse;
import com.example.storyai.chapter.model.GenerationJob;
import com.example.storyai.chapter.service.GenerationJobService;
import com.example.storyai.chapter.service.GenerationOrchestrationService;

/**
 * REST API for multi-chapter generation jobs (M5 / TASK-036..040, AT-L01/L02/M01).
 *
 * <pre>
 * POST /api/stages/{stageId}/generate?mode=STEP|CONTINUOUS  -> start a job
 * POST /api/generation-jobs/{jobId}/continue                -> STEP: next chapter
 * POST /api/generation-jobs/{jobId}/retry                   -> FAILED: restart
 * GET  /api/generation-jobs/{jobId}                         -> job status
 * GET  /api/stages/{stageId}/generation-jobs                -> latest job for stage
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class GenerationController {

    private final GenerationOrchestrationService orchestrationService;
    private final GenerationJobService jobService;

    public GenerationController(GenerationOrchestrationService orchestrationService,
                                GenerationJobService jobService) {
        this.orchestrationService = orchestrationService;
        this.jobService = jobService;
    }

    @PostMapping("/stages/{stageId}/generate")
    public GenerationJobResponse start(@PathVariable Long stageId,
                                        @RequestParam(defaultValue = "CONTINUOUS") String mode) {
        GenerationJob.Mode parsed = parseMode(mode);
        return new GenerationJobResponse(orchestrationService.startJob(stageId, parsed));
    }

    @PostMapping("/generation-jobs/{jobId}/continue")
    public GenerationJobResponse cont(@PathVariable Long jobId) {
        return new GenerationJobResponse(orchestrationService.continueJob(jobId));
    }

    @PostMapping("/generation-jobs/{jobId}/retry")
    public GenerationJobResponse retry(@PathVariable Long jobId) {
        return new GenerationJobResponse(orchestrationService.retryJob(jobId));
    }

    @PostMapping("/generation-jobs/{jobId}/pause")
    public GenerationJobResponse pause(@PathVariable Long jobId) {
        return new GenerationJobResponse(orchestrationService.requestPause(jobId));
    }

    @PostMapping("/generation-jobs/{jobId}/stop")
    public GenerationJobResponse stop(@PathVariable Long jobId) {
        return new GenerationJobResponse(orchestrationService.requestStop(jobId));
    }

    @GetMapping("/generation-jobs/{jobId}")
    public GenerationJobResponse get(@PathVariable Long jobId) {
        return new GenerationJobResponse(jobService.get(jobId));
    }

    @GetMapping("/stages/{stageId}/generation-jobs")
    public List<GenerationJobResponse> listByStage(@PathVariable Long stageId) {
        GenerationJob latest = jobService.findLatestByStage(stageId);
        if (latest == null) {
            return List.of();
        }
        return List.of(new GenerationJobResponse(latest));
    }

    private GenerationJob.Mode parseMode(String mode) {
        try {
            return GenerationJob.Mode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return GenerationJob.Mode.CONTINUOUS;
        }
    }
}
