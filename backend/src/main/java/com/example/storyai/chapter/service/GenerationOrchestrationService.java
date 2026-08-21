package com.example.storyai.chapter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.chapter.model.GenerationJob;
import com.example.storyai.common.exception.NoPendingChapterException;
import com.example.storyai.stage.service.StageService;

/**
 * Multi-chapter generation orchestration (M5 / TASK-037..040, AT-L01/L02/M01/M02/M03).
 *
 * <p><b>One reusable unit:</b> both STEP and CONTINUOUS mode drive the exact same
 * single-chapter flow — {@link ChapterGenerationService#generateNextChapter(Long)}
 * (which already does Chapter -&gt; Memory -&gt; Checkpoint). There is no second
 * generation implementation.</p>
 *
 * <p><b>STEP mode:</b> generate one chapter, then set status PAUSED and return.
 * The author calls {@link #continueJob(Long)} to produce the next chapter. When no
 * pending plan remains the job becomes COMPLETED.</p>
 *
 * <p><b>CONTINUOUS mode:</b> loop Chapter -&gt; Memory -&gt; Checkpoint -&gt; Next
 * until the stage has no pending plan (COMPLETED) or a failure occurs (FAILED).</p>
 *
 * <p><b>Failure / retry (TASK-040):</b> any exception from the single-chapter flow
 * marks the job FAILED with {@code lastError} and the stack unwinds. The chapter
 * that triggered the failure was NOT persisted (the writer/extractor step throws
 * before save, or extraction fails after save but before next chapter), so retrying
 * re-runs the same pending plan. {@link #retryJob(Long)} restarts a FAILED job from
 * its current index.</p>
 */
@Service
public class GenerationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationOrchestrationService.class);

    private final ChapterGenerationService generationService;
    private final StageService stageService;
    private final GenerationJobService jobService;

    public GenerationOrchestrationService(ChapterGenerationService generationService,
                                          StageService stageService,
                                          GenerationJobService jobService) {
        this.generationService = generationService;
        this.stageService = stageService;
        this.jobService = jobService;
    }

    /** Starts a generation job for a stage in the given mode (STEP or CONTINUOUS). */
    public GenerationJob startJob(Long stageId, GenerationJob.Mode mode) {
        int total = stageService.getPlans(stageId).size();
        GenerationJob job = new GenerationJob();
        job.setStageId(stageId);
        job.setMode(mode.name());
        job.setCurrentPlanIndex(0);
        job.setTotal(total);
        job.setStatus(GenerationJob.Status.PENDING.name());
        job.setPhase(GenerationJob.Phase.PLANNING.name());
        job.setLastError(null);
        GenerationJob saved = jobService.create(job);

        try {
            markRunning(saved);
            if (mode == GenerationJob.Mode.CONTINUOUS) {
                return runContinuous(saved);
            }
            return runStep(saved);
        } catch (RuntimeException ex) {
            return fail(saved, ex);
        }
    }

    /** Resumes a PAUSED job (author clicked Continue). STEP runs one more chapter;
     *  CONTINUOUS runs to completion. COMPLETED/FAILED jobs are returned unchanged. */
    public GenerationJob continueJob(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        if (GenerationJob.Status.COMPLETED.name().equals(job.getStatus())
                || GenerationJob.Status.FAILED.name().equals(job.getStatus())) {
            return job;
        }
        try {
            markRunning(job);
            if (GenerationJob.Mode.CONTINUOUS.name().equals(job.getMode())) {
                return runContinuous(job);
            }
            return runStep(job);
        } catch (RuntimeException ex) {
            return fail(job, ex);
        }
    }

    /** Retries a FAILED job from its current index (the failing plan was never saved). */
    public GenerationJob retryJob(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        if (!GenerationJob.Status.FAILED.name().equals(job.getStatus())) {
            return job;
        }
        job.setLastError(null);
        GenerationJob saved = jobService.update(job);
        try {
            markRunning(saved);
            if (GenerationJob.Mode.CONTINUOUS.name().equals(saved.getMode())) {
                return runContinuous(saved);
            }
            return runStep(saved);
        } catch (RuntimeException ex) {
            return fail(saved, ex);
        }
    }

    // ---- internal step engine ----

    /** STEP: exactly one chapter, then PAUSED (or COMPLETED if that was the last plan). */
    private GenerationJob runStep(GenerationJob job) {
        generateOneStep(job); // throws NoPendingChapterException only if already complete
        if (job.getCurrentPlanIndex() >= job.getTotal()) {
            return complete(job);
        }
        job.setStatus(GenerationJob.Status.PAUSED.name());
        job.setPhase(GenerationJob.Phase.CHECKPOINT.name());
        return jobService.update(job);
    }

    /** CONTINUOUS: loop until the stage has no pending plan, or failure. */
    private GenerationJob runContinuous(GenerationJob job) {
        while (true) {
            try {
                generateOneStep(job); // throws NoPendingChapterException when done
            } catch (NoPendingChapterException ex) {
                return complete(job);
            }
            // loop continues immediately (no checkpoint wait)
        }
    }

    /** The single reusable generation step shared by both modes. */
    private void generateOneStep(GenerationJob job) {
        job.setPhase(GenerationJob.Phase.WRITING.name());
        jobService.update(job);
        // Reuses the exact M3+M4 single-chapter flow (Chapter -> Memory -> Checkpoint).
        generationService.generateNextChapter(job.getStageId());
        job.setCurrentPlanIndex(job.getCurrentPlanIndex() + 1);
        job.setPhase(GenerationJob.Phase.MEMORY.name());
        jobService.update(job);
    }

    private void markRunning(GenerationJob job) {
        job.setStatus(GenerationJob.Status.RUNNING.name());
        jobService.update(job);
    }

    private GenerationJob complete(GenerationJob job) {
        job.setStatus(GenerationJob.Status.COMPLETED.name());
        job.setPhase(GenerationJob.Phase.CHECKPOINT.name());
        job.setLastError(null);
        return jobService.update(job);
    }

    private GenerationJob fail(GenerationJob job, RuntimeException ex) {
        log.error("Generation job {} failed: {}", job.getId(), ex.getMessage(), ex);
        job.setStatus(GenerationJob.Status.FAILED.name());
        job.setLastError(truncate(ex.getMessage() != null ? ex.getMessage() : ex.toString(), 2000));
        return jobService.update(job);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
