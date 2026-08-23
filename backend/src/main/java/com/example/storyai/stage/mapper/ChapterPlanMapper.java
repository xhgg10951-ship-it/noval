package com.example.storyai.stage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.stage.model.ChapterPlan;

/**
 * MyBatis mapper for the chapter_plan table (TASK-012).
 */
@Mapper
public interface ChapterPlanMapper {

    int insertBatch(@Param("list") List<ChapterPlan> plans);

    List<ChapterPlan> findByStageId(@Param("stageId") Long stageId);

    /** TASK-135: only active, non-completed plans — the eligible generation queue. */
    List<ChapterPlan> findActiveRemaining(@Param("stageId") Long stageId);

    ChapterPlan findById(@Param("id") Long id);

    int updateGoal(@Param("id") Long id, @Param("goal") String goal);

    int updateEditable(@Param("id") Long id,
                       @Param("goal") String goal,
                       @Param("expectedProgress") String expectedProgress,
                       @Param("targetCharacters") Integer targetCharacters,
                       @Param("mustAdvance") String mustAdvance,
                       @Param("mustNotDo") String mustNotDo,
                       @Param("storyBeats") String storyBeats,
                       @Param("endingIntent") String endingIntent);

    /** TASK-134/135: mark a single plan COMPLETED once its chapter is generated. */
    int markCompleted(@Param("id") Long id);

    /** TASK-134: mark all currently-active plans as superseded (preserves history). */
    int supersedeRemaining(@Param("stageId") Long stageId);

    int deleteByStageId(@Param("stageId") Long stageId);
}
