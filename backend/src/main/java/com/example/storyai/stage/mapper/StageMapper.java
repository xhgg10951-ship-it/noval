package com.example.storyai.stage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.stage.model.Stage;

/**
 * MyBatis mapper for the stage table (TASK-012).
 */
@Mapper
public interface StageMapper {

    int insert(Stage stage);

    Stage findById(@Param("id") Long id);

    List<Stage> findByStoryId(@Param("storyId") Long storyId);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updatePlanCounts(@Param("id") Long id,
                         @Param("suggestedChapterCount") Integer suggestedChapterCount,
                         @Param("targetChapterCount") Integer targetChapterCount);
}
