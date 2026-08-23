package com.example.storyai.chapter.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.chapter.mapper.ChapterMapper;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.chapter.model.MemoryExtractionStatus;
import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.service.StageService;

/**
 * TASK-125 — Next Safe Action Resolver.
 *
 * <p>Recovery decisions are made from DATABASE FACTS, not from an in-memory
 * {@code currentPlanIndex}. This closes the RC-08 hole: a chapter that was
 * persisted but whose memory extraction FAILED/PENDING/STALE must be re-extracted
 * for the SAME chapter, never silently skipped to the next plan.</p>
 *
 * <pre>
 *   No chapter for next plan ............ GENERATE
 *   Chapter exists, memory FAILED ....... EXTRACT_MEMORY (retry same chapter)
 *   Chapter exists, memory PENDING ....... EXTRACT_MEMORY (finish same chapter)
 *   Chapter exists, memory STALE ......... EXTRACT_MEMORY (reconcile, TASK-148)
 *   Chapter complete (COMPLETED) ........ NEXT_PLAN (or COMPLETE if none left)
 * </pre>
 */
@Service
public class NextSafeActionResolver {

    private static final Logger log = LoggerFactory.getLogger(NextSafeActionResolver.class);

    public enum SafeAction {
        GENERATE,          // produce a brand-new chapter for the next pending plan
        EXTRACT_MEMORY,    // re-run memory extraction for an existing chapter
        NEXT_PLAN,         // advance to the next pending plan (current one fully done)
        COMPLETE           // nothing left to do; the stage is finished
    }

    private final ChapterMapper chapterMapper;
    private final StageService stageService;

    public NextSafeActionResolver(ChapterMapper chapterMapper,
                                  StageService stageService) {
        this.chapterMapper = chapterMapper;
        this.stageService = stageService;
    }

    /**
     * Given a stage, decides the next safe action based on persisted chapters and
     * their extraction status. Pure read-only; never mutates state.
     */
    public SafeAction resolve(Long stageId) {
        // TASK-137 fix: pending-ness is judged against the ACTIVE REMAINING queue
        // only. Superseded (replanned-away) and completed rows are history — a
        // superseded plan without a chapter must never look like "work left to do".
        List<ChapterPlan> activePlans = stageService.getActiveRemainingPlans(stageId);
        List<Chapter> chapters = chapterMapper.findByStageId(stageId);

        // Generated plan ids (a plan with a chapter is "started").
        java.util.Set<Long> startedPlanIds = new java.util.HashSet<>();
        Chapter lastChapter = null;
        for (Chapter c : chapters) {
            if (c.getPlanId() != null) {
                startedPlanIds.add(c.getPlanId());
            }
            if (lastChapter == null
                    || c.getChapterNumber() > lastChapter.getChapterNumber()
                    || (c.getChapterNumber() == lastChapter.getChapterNumber()
                        && c.getId() > lastChapter.getId())) {
                lastChapter = c;
            }
        }

        // Every persisted chapter is part of the release checkpoint, not only
        // the newest row. An author may revise an earlier chapter while a later
        // Writer call is in flight; that earlier STALE/FAILED/PENDING memory must
        // be reconciled before another plan or Stage completion.
        Chapter unfinished = chapters.stream()
                .filter(c -> !MemoryExtractionStatus.COMPLETED.equals(
                        c.getMemoryExtractionStatus()))
                .max(java.util.Comparator.comparing(Chapter::getChapterNumber)
                        .thenComparing(Chapter::getId))
                .orElse(null);
        if (unfinished != null) {
            log.info("Stage {}: chapter {} extraction status={} -> EXTRACT_MEMORY",
                    stageId, unfinished.getId(), unfinished.getMemoryExtractionStatus());
            return SafeAction.EXTRACT_MEMORY;
        }

        // Active plans not yet started at all -> generate the next one.
        boolean hasPendingPlan = activePlans.stream()
                .anyMatch(p -> !startedPlanIds.contains(p.getId()));
        if (lastChapter == null) {
            return hasPendingPlan ? SafeAction.GENERATE : SafeAction.COMPLETE;
        }

        // Last chapter fully done: either advance to next plan or finish.
        return hasPendingPlan ? SafeAction.NEXT_PLAN : SafeAction.COMPLETE;
    }
}
