package com.example.storyai.story.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.story.mapper.StoryConstraintMapper;
import com.example.storyai.story.mapper.StoryMapper;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;

/**
 * Core business layer for the Story domain (ARCHITECTURE §24).
 *
 * <p>Holds the create/get/list orchestration that uses the MyBatis mappers.
 * Mappers do raw DB I/O only; the decision of "insert story then its
 * constraints in one transaction" lives here.
 */
@Service
public class StoryService {

    private final StoryMapper storyMapper;
    private final StoryConstraintMapper constraintMapper;

    public StoryService(StoryMapper storyMapper, StoryConstraintMapper constraintMapper) {
        this.storyMapper = storyMapper;
        this.constraintMapper = constraintMapper;
    }

    /**
     * Creates a story and its constraints atomically.
     *
     * @return the story with its generated id populated.
     */
    @Transactional
    public Story createStory(Story story, List<StoryConstraint> constraints) {
        storyMapper.insert(story);
        if (constraints != null && !constraints.isEmpty()) {
            constraints.forEach(c -> c.setStoryId(story.getId()));
            constraintMapper.insertBatch(constraints);
        }
        return story;
    }

    /** Loads a story by id or throws if missing. */
    public Story getStory(Long id) {
        Story story = storyMapper.findById(id);
        if (story == null) {
            throw new ResourceNotFoundException("Story", id);
        }
        return story;
    }

    public List<StoryConstraint> getConstraints(Long storyId) {
        return constraintMapper.findByStoryId(storyId);
    }

    public List<Story> listStories() {
        return storyMapper.findAll();
    }

    /**
     * TASK-150 — partial update of the author's writing settings.
     * NULL request fields mean "leave unchanged"; the target chapter count is
     * the long-form pace anchor consumed by Phase 6 (Planner position).
     */
    @Transactional
    public Story updateWritingSettings(Long storyId, Integer defaultTargetCharacters,
                                       String writingStyle, Integer targetChapterCount) {
        getStory(storyId); // 404 guard
        if (targetChapterCount != null && (targetChapterCount < 1 || targetChapterCount > 5000)) {
            throw new IllegalArgumentException("目标章节数需在 1–5000 之间");
        }
        storyMapper.updateWritingSettings(storyId, defaultTargetCharacters,
                writingStyle, targetChapterCount);
        return getStory(storyId);
    }
}
