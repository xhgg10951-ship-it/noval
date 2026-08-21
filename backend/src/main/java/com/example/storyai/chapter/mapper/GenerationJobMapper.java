package com.example.storyai.chapter.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.chapter.model.GenerationJob;

/** MyBatis mapper for generation jobs (M5 / TASK-036). */
@Mapper
public interface GenerationJobMapper {

    int insert(GenerationJob job);

    GenerationJob findById(Long id);

    GenerationJob findLatestByStage(@Param("stageId") Long stageId);

    void update(GenerationJob job);
}
