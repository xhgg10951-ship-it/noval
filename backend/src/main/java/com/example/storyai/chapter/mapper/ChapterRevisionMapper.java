package com.example.storyai.chapter.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.chapter.model.ChapterRevision;

/** MyBatis mapper for chapter revisions (v0.1.1 Phase 5 / TASK-140). */
@Mapper
public interface ChapterRevisionMapper {

    int insert(ChapterRevision revision);

    ChapterRevision findById(@Param("id") Long id);

    /** All revisions of a chapter, newest first (history view). */
    List<ChapterRevision> findByChapterId(@Param("chapterId") Long chapterId);

    Integer findMaxVersion(@Param("chapterId") Long chapterId);
}
