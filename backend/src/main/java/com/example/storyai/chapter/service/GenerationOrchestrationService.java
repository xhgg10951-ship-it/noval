package com.example.storyai.chapter.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.chapter.model.GenerationJob;
import com.example.storyai.chapter.mapper.ChapterMapper;
import com.example.storyai.chapter.model.Chapter;
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
    private final NextSafeActionResolver safeActionResolver;
    private final ChapterMapper chapterMapper;
    // TASK-127: application-managed background executor. Continuous generation no
    // longer blocks the HTTP request — POST /generate returns the Job immediately
    // and the chapters run on this pool. No MQ / Redis / new microservice.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GenerationOrchestrationService(ChapterGenerationService generationService,
                                          StageService stageService,
                                          GenerationJobService jobService,
                                          NextSafeActionResolver safeActionResolver,
                                          ChapterMapper chapterMapper) {
        this.generationService = generationService;
        this.stageService = stageService;
        this.jobService = jobService;
        this.safeActionResolver = safeActionResolver;
        this.chapterMapper = chapterMapper;
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

        // TASK-127: run off the HTTP thread. The caller gets the job row back
        // immediately (PENDING -> RUNNING) and polls GET /generation-jobs/{id}.
        submitRun(saved.getId());
        return jobService.get(saved.getId());
    }

    /** Resumes a PAUSED job (author clicked Continue). STEP runs one more chapter;
     *  CONTINUOUS runs to completion. COMPLETED/FAILED jobs are returned unchanged. */
    public GenerationJob continueJob(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        if (GenerationJob.Status.COMPLETED.name().equals(job.getStatus())
                || GenerationJob.Status.FAILED.name().equals(job.getStatus())) {
            return job;
        }
        submitRun(jobId);
        return jobService.get(jobId);
    }

    /** Retries a FAILED job from its current index (the failing plan was never saved). */
    public GenerationJob retryJob(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        if (!GenerationJob.Status.FAILED.name().equals(job.getStatus())) {
            return job;
        }
        job.setLastError(null);
        jobService.update(job);
        submitRun(jobId);
        return jobService.get(jobId);
    }

    /**
     * TASK-127 — dispatches the run to the background executor. The runnable reloads
     * the job by id inside the worker thread so the HTTP thread never blocks on the
     * (possibly long) CONTINUOUS loop. All status transitions are persisted, so a
     * crashed worker leaves an inspectable FAILED row.
     */
    private void submitRun(Long jobId) {
        GenerationJob.Mode mode = GenerationJob.Mode.valueOf(jobService.get(jobId).getMode());
        executor.submit(() -> {
            GenerationJob job = jobService.get(jobId);
            try {
                markRunning(job);
                if (mode == GenerationJob.Mode.CONTINUOUS) {
                    runContinuous(job);
                } else {
                    runStep(job);
                }
            } catch (RuntimeException ex) {
                fail(job, ex);
            }
        });
    }

    /** TASK-129 — author requests PAUSE; loop stops at the next checkpoint. */
    public GenerationJob requestPause(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        job.setPauseRequested(true);
        job.setStopRequested(false);
        return jobService.update(job);
    }

    /** TASK-130 — author requests STOP; loop terminates, chapters retained. */
    public GenerationJob requestStop(Long jobId) {
        GenerationJob job = jobService.get(jobId);
        job.setStopRequested(true);
        job.setPauseRequested(false);
        return jobService.update(job);
    }

    // ---- internal step engine ----

    /** Reads the freshest control signals for a job (reloaded from DB). */
    private boolean isStopRequested(Long jobId) {
        Boolean v = jobService.get(jobId).getStopRequested();
        return Boolean.TRUE.equals(v);
    }

    private boolean isPauseRequested(Long jobId) {
        Boolean v = jobService.get(jobId).getPauseRequested();
        return Boolean.TRUE.equals(v);
    }

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

    /** CONTINUOUS: loop until the stage has no pending plan, or a stop/pause/failure. */
    private GenerationJob runContinuous(GenerationJob job) {
        while (true) {
            if (isStopRequested(job.getId())) {
                return stop(job);
            }
            if (isPauseRequested(job.getId())) {
                return pause(job);
            }
            try {
                generateOneStep(job); // throws NoPendingChapterException when done
            } catch (NoPendingChapterException ex) {
                return complete(job);
            }
            // loop continues immediately (no checkpoint wait)
        }
    }

    // ---- internal step engine ----

    /** The single reusable generation step shared by both modes. */
    private void generateOneStep(GenerationJob job) {
        job.setPhase(GenerationJob.Phase.WRITING.name());
        jobService.update(job);

        // TASK-125: recovery is decided from DB facts, not currentPlanIndex alone.
        // If the last persisted chapter's extraction is incomplete, retry EXTRACTION
        // on the SAME chapter (safe), never skip to the next plan.
        NextSafeActionResolver.SafeAction action = safeActionResolver.resolve(job.getStageId());
        if (action == NextSafeActionResolver.SafeAction.EXTRACT_MEMORY) {
            Chapter last = lastUnfinishedChapter(job.getStageId());
            if (last != null) {
                job.setPhase(GenerationJob.Phase.MEMORY.name());
                jobService.update(job);
                generationService.reExtractChapter(last.getId());
                return; // same plan index; next step will resolve to NEXT_PLAN/COMPLETE
            }
        }

        // Reuses the exact M3+M4 single-chapter flow (Chapter -> Memory -> Checkpoint).
        generationService.generateNextChapter(job.getStageId());
        job.setCurrentPlanIndex(job.getCurrentPlanIndex() + 1);
        job.setPhase(GenerationJob.Phase.MEMORY.name());
        jobService.update(job);
    }

    /** Returns the most recent chapter whose extraction is not COMPLETED, if any. */
    private Chapter lastUnfinishedChapter(Long stageId) {
        return chapterMapper.findByStageId(stageId).stream()
                .filter(c -> !com.example.storyai.chapter.model.MemoryExtractionStatus.COMPLETED
                        .equals(c.getMemoryExtractionStatus()))
                .max(java.util.Comparator.comparing(Chapter::getChapterNumber))
                .orElse(null);
    }

    private void markRunning(GenerationJob job) {
        job.setStatus(GenerationJob.Status.RUNNING.name());
        jobService.update(job);
    }

    private GenerationJob complete(GenerationJob job) {
        job.setStatus(GenerationJob.Status.COMPLETED.name());
        job.setPhase(GenerationJob.Phase.CHECKPOINT.name());
        job.setLastError(null);
        job.setPauseRequested(false);
        job.setStopRequested(false);
        GenerationJob saved = jobService.update(job);
        // TASK-131: transition the Stage to COMPLETED only once all chapters have a
        // stable memory extraction state. completeStage is idempotent and guarded,
        // so re-running completion cannot leave the Stage lifecycle inconsistent.
        stageService.completeStage(job.getStageId());
        return saved;
    }

    /** TASK-129 — loop reached a checkpoint with a pending pause request. */
    private GenerationJob pause(GenerationJob job) {
        job.setStatus(GenerationJob.Status.PAUSED.name());
        job.setPhase(GenerationJob.Phase.CHECKPOINT.name());
        job.setPauseRequested(false);
        return jobService.update(job);
    }

    /** TASK-130 — loop reached a checkpoint with a stop request; chapters retained. */
    private GenerationJob stop(GenerationJob job) {
        job.setStatus(GenerationJob.Status.STOPPED.name());
        job.setPhase(GenerationJob.Phase.CHECKPOINT.name());
        job.setStopRequested(false);
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
