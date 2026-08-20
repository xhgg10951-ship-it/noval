package com.example.storyai.story.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.storyai.story.model.StoryConstraint;

/**
 * MyBatis mapper for the {@code story_constraint} table (TASK-008).
 */
@Mapper
public interface StoryConstraintMapper {

    /** Batch-inserts constraints; each must have storyId set. */
    int insertBatch(List<StoryConstraint> constraints);

    List<StoryConstraint> findByStoryId(Long storyId);
}
