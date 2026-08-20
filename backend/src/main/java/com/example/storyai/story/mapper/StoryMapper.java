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
}
