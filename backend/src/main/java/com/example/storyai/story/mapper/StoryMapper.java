package com.example.storyai.story.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.storyai.story.model.Story;

/**
 * MyBatis mapper for the {@code story} table (TASK-008).
 * Only DB read/write — no business decisions here (ARCHITECTURE §25).
 */
@Mapper
public interface StoryMapper {

    /** Inserts a story and populates the generated id. */
    int insert(Story story);

    Story findById(Long id);

    List<Story> findAll();

    /** TASK-150: partial writing-settings update; NULL fields are left unchanged. */
    int updateWritingSettings(@org.apache.ibatis.annotations.Param("id") Long id,
                              @org.apache.ibatis.annotations.Param("defaultTargetCharacters") Integer defaultTargetCharacters,
                              @org.apache.ibatis.annotations.Param("writingStyle") String writingStyle,
                              @org.apache.ibatis.annotations.Param("targetChapterCount") Integer targetChapterCount);
}
