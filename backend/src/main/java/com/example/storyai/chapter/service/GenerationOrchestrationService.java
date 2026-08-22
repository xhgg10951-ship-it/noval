package com.example.storyai.chapter.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.chapter.model.GenerationJob;
import com.example.storyai.chapter.mapper.ChapterMapper;
import com.example.storyai.chapter.mapper.GenerationJobMapper;
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
 * <p><b>TASK-127:</b> runs on an application-managed background executor — POST
 * returns immediately and the client polls GET /generation-jobs/{id}. No MQ.</p>
 *
 * <p><b>TASK-132 fix:</b> the worker never persists a long-lived in-memory copy of
 * the row. The previous full-row UPDATE let a stale copy clobber concurrently
 * written control signals (a pause/stop request was silently erased by the
 * per-chapter progress write). Writes are now narrow and disjoint: progress
 * heartbeats, control-signal flips, and terminal transitions touch disjoint
 * columns; mutable state is always re-read from MySQL between steps.</p>
 */
@Service
public class GenerationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationOrchestrationService.class);

    private final ChapterGenerationService generationService;
    private final StageService stageService;
    private final GenerationJobService jobService;
    private final GenerationJobMapper jobMapper;
    private final NextSafeActionResolver safeActionResolver;
    private final ChapterMapper chapterMapper;
    // TASK-127: application-managed background executor. Continuous generation no
    // longer blocks the HTTP request — POST /generate returns the Job immediately
    // and the chapters run on this pool. No MQ / Redis / new microservice.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GenerationOrchestrationService(ChapterGenerationService generationService,
                                          StageService stageService,
                                          GenerationJobService jobService,
                                          GenerationJobMapper jobMapper,
                                          NextSafeActionResolver safeActionResolver,
                                          ChapterMapper chapterMapper) {
        this.generationService = generationService;
        this.stageService = stageService;
        this.jobService = jobService;
        this.jobMapper = jobMapper;
        this.safeActionResolver = safeActionResolver;
        this.chapterMapper = chapterMapper;
    }

    /** Starts a generation job for a stage in the given mode (STEP or CONTINUOUS). */
    public GenerationJob startJob(Long stageId, GenerationJob.Mode mode) {
        // TASK-137: total counts only the ACTIVE REMAINING queue — superseded and
        // completed plan rows are history and must not inflate the progress bar.
        int total = stageService.getActiveRemainingPlans(stageId).size();
        GenerationJob job = new GenerationJob();
        job.setStageId(stageId);
        job.setMode(mode.name());
        job.setCurrentPlanIndex(0);
        job.setTotal(total);
        job.setStatus(GenerationJob.Status.PENDING.name());
        job.setPhase(GenerationJob.Phase.PLANNING.name());
        job.setLastError(null);
        // TASK-129/130: control signals start clean — the columns are NOT NULL and
        // a fresh job must never inherit a stale pause/stop request.
        job.setPauseRequested(false);
        job.setStopRequested(false);
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
        submitRun(jobId); // markRunning clears lastError when the worker picks it up
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
        Long stageId = jobService.get(jobId).getStageId();
        executor.submit(() -> {
            try {
                markRunning(jobId);
                if (mode == GenerationJob.Mode.CONTINUOUS) {
                    runContinuous(jobId, stageId);
                } else {
                    runStep(jobId, stageId);
                }
            } catch (RuntimeException ex) {
                fail(jobId, ex);
            }
        });
    }

    /** TASK-129 — author requests PAUSE; loop stops at the next checkpoint. */
    public GenerationJob requestPause(Long jobId) {
        jobMapper.updateControlSignals(jobId, true, false);
        return jobService.get(jobId);
    }

    /** TASK-130 — author requests STOP; loop terminates, chapters retained. */
    public GenerationJob requestStop(Long jobId) {
        jobMapper.updateControlSignals(jobId, false, true);
        return jobService.get(jobId);
    }

    // ---- internal step engine ----

    /** STEP: exactly one chapter, then PAUSED (or COMPLETED if that was the last plan). */
    private GenerationJob runStep(Long jobId, Long stageId) {
        try {
            generateOneStep(jobId, stageId); // throws NoPendingChapterException when done
        } catch (NoPendingChapterException ex) {
            // same convergence as CONTINUOUS: an empty active queue means DONE
            // (e.g. a Replan Remaining shrank the remainder mid-job — TASK-137).
            return complete(jobId);
        }
        // TASK-137 fix: completion is decided from DB FACTS (no active plan left),
        // never from a stale in-flight `total`. A Replan Remaining mid-job changes
        // the queue size, so index-vs-total would either never finish or stop early.
        if (safeActionResolver.resolve(stageId) == NextSafeActionResolver.SafeAction.COMPLETE) {
            return complete(jobId);
        }
        finalizeStatus(jobId, GenerationJob.Status.PAUSED.name(),
                GenerationJob.Phase.CHECKPOINT.name(), null, true, false);
        return jobService.get(jobId);
    }

    /** CONTINUOUS: loop until the stage has no pending plan, or a stop/pause/failure. */
    private GenerationJob runContinuous(Long jobId, Long stageId) {
        while (true) {
            if (isStopRequested(jobId)) {
                return stop(jobId);
            }
            if (isPauseRequested(jobId)) {
                return pause(jobId);
            }
            try {
                generateOneStep(jobId, stageId); // throws NoPendingChapterException when done
            } catch (NoPendingChapterException ex) {
                return complete(jobId);
            }
            // loop continues immediately (no checkpoint wait)
        }
    }

    // ---- internal step engine ----

    /**
     * The single reusable generation step shared by both modes. Progress is read
     * from MySQL and written back with narrow updates only (TASK-132).
     */
    private void generateOneStep(Long jobId, Long stageId) {
        jobMapper.updateProgress(jobId, currentIndex(jobId), GenerationJob.Phase.WRITING.name());

        // TASK-125: recovery is decided from DB facts, not currentPlanIndex alone.
        // If the last persisted chapter's extraction is incomplete, retry EXTRACTION
        // on the SAME chapter (safe), never skip to the next plan.
        NextSafeActionResolver.SafeAction action = safeActionResolver.resolve(stageId);
        if (action == NextSafeActionResolver.SafeAction.EXTRACT_MEMORY) {
            Chapter last = lastUnfinishedChapter(stageId);
            if (last != null) {
                jobMapper.updateProgress(jobId, currentIndex(jobId),
                        GenerationJob.Phase.MEMORY.name());
                generationService.reExtractChapter(last.getId());
                return; // same plan index; next step will resolve to NEXT_PLAN/COMPLETE
            }
        }

        // Reuses the exact M3+M4 single-chapter flow (Chapter -> Memory -> Checkpoint).
        generationService.generateNextChapter(stageId);
        jobMapper.updateProgress(jobId, currentIndex(jobId) + 1,
                GenerationJob.Phase.MEMORY.name());
    }

    private int currentIndex(Long jobId) {
        return jobService.get(jobId).getCurrentPlanIndex();
    }

    /** Returns the most recent chapter whose extraction is not COMPLETED, if any. */
    private Chapter lastUnfinishedChapter(Long stageId) {
        return chapterMapper.findByStageId(stageId).stream()
                .filter(c -> !com.example.storyai.chapter.model.MemoryExtractionStatus.COMPLETED
                        .equals(c.getMemoryExtractionStatus()))
                .max(java.util.Comparator.comparing(Chapter::getChapterNumber))
                .orElse(null);
    }

    private void markRunning(Long jobId) {
        // also clears a stale lastError from a previous failed attempt (retry path)
        finalizeStatus(jobId, GenerationJob.Status.RUNNING.name(),
                GenerationJob.Phase.PLANNING.name(), null, false, false);
    }

    private boolean isStopRequested(Long jobId) {
        Boolean v = jobService.get(jobId).getStopRequested();
        return Boolean.TRUE.equals(v);
    }

    private boolean isPauseRequested(Long jobId) {
        Boolean v = jobService.get(jobId).getPauseRequested();
        return Boolean.TRUE.equals(v);
    }

    private GenerationJob complete(Long jobId) {
        // TASK-131/132: transition the Stage BEFORE the Job reads COMPLETED.
        // Once a poller observes Job=COMPLETED, the Stage lifecycle is already
        // converged (AC-113 observation consistency). completeStage is idempotent
        // and guarded: it refuses to flip while any chapter memory is unstable,
        // and that failure now surfaces as a FAILED job instead of a silent split.
        Long stageId = jobService.get(jobId).getStageId();
        try {
            stageService.completeStage(stageId);
        } catch (RuntimeException ex) {
            fail(jobId, ex);
            throw ex;
        }
        finalizeStatus(jobId, GenerationJob.Status.COMPLETED.name(),
                GenerationJob.Phase.CHECKPOINT.name(), null, true, true);
        return jobService.get(jobId);
    }

    /** TASK-129 — loop reached a checkpoint with a pending pause request. */
    private GenerationJob pause(Long jobId) {
        finalizeStatus(jobId, GenerationJob.Status.PAUSED.name(),
                GenerationJob.Phase.CHECKPOINT.name(), null, true, false);
        return jobService.get(jobId);
    }

    /** TASK-130 — loop reached a checkpoint with a stop request; chapters retained. */
    private GenerationJob stop(Long jobId) {
        finalizeStatus(jobId, GenerationJob.Status.STOPPED.name(),
                GenerationJob.Phase.CHECKPOINT.name(), null, false, true);
        return jobService.get(jobId);
    }

    private GenerationJob fail(Long jobId, RuntimeException ex) {
        log.error("Generation job {} failed: {}", jobId, ex.getMessage(), ex);
        String phase;
        try {
            phase = jobService.get(jobId).getPhase();
        } catch (RuntimeException readEx) {
            phase = GenerationJob.Phase.WRITING.name();
        }
        finalizeStatus(jobId, GenerationJob.Status.FAILED.name(), phase,
                truncate(ex.getMessage() != null ? ex.getMessage() : ex.toString(), 2000),
                false, false);
        return jobService.get(jobId);
    }

    /** Narrow terminal/status write (TASK-132): status+phase+lastError (+flag clears). */
    private void finalizeStatus(Long jobId, String status, String phase, String lastError,
                                boolean clearPause, boolean clearStop) {
        jobMapper.updateTerminal(jobId, status, phase, lastError, clearPause, clearStop);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
