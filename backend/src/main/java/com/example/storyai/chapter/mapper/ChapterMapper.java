package com.example.storyai.chapter.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.chapter.model.Chapter;

/** MyBatis mapper for generated chapters (M3 / TASK-019). */
@Mapper
public interface ChapterMapper {

    int insert(Chapter chapter);

    Chapter findById(Long id);

    Chapter findByPlanId(Long planId);

    List<Chapter> findByStageId(@Param("stageId") Long stageId);

    List<Chapter> findByStoryId(Long storyId);

    /** Highest chapter_number already generated for the story (0 if none). */
    int maxChapterNumber(@Param("storyId") Long storyId);
}
