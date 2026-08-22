package com.example.storyai.chapter.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.chapter.model.GenerationJob;

/** MyBatis mapper for generation jobs (M5 / TASK-036). */
@Mapper
public interface GenerationJobMapper {

    int insert(GenerationJob job);

    GenerationJob findById(@Param("id") Long id);

    GenerationJob findLatestByStage(@Param("stageId") Long stageId);

    /** Legacy full-row update (kept for compatibility; orchestration no longer uses it). */
    int update(GenerationJob job);

    /**
     * TASK-132 fix: narrow progress heartbeat (index + phase only), so a stale
     * in-memory job copy can never clobber concurrently written control signals.
     */
    int updateProgress(@Param("id") Long id,
                       @Param("currentPlanIndex") int currentPlanIndex,
                       @Param("phase") String phase);

    /** TASK-132 fix: author control signals, written in isolation. */
    int updateControlSignals(@Param("id") Long id,
                             @Param("pauseRequested") Boolean pauseRequested,
                             @Param("stopRequested") Boolean stopRequested);

    /**
     * TASK-132 fix: status/phase/lastError convergence with optional flag clears
     * (used by COMPLETED/PAUSED/STOPPED to reset consumed signals).
     */
    int updateTerminal(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("phase") String phase,
                       @Param("lastError") String lastError,
                       @Param("clearPause") boolean clearPause,
                       @Param("clearStop") boolean clearStop);
}
