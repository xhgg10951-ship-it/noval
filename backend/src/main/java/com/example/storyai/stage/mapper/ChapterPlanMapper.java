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

    ChapterPlan findById(@Param("id") Long id);

    int updateGoal(@Param("id") Long id, @Param("goal") String goal);

    int deleteByStageId(@Param("stageId") Long stageId);
}
