package com.example.storyai.chapter.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.chapter.mapper.ChapterMapper;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.common.exception.ResourceNotFoundException;

/**
 * Transactional persistence for generated chapters (TASK-019).
 *
 * <p>Deliberately separate from {@link ChapterGenerationService}: the AI HTTP
 * call happens OUTSIDE any DB transaction; only these methods run
 * transactionally.</p>
 */
@Service
public class ChapterService {

    private final ChapterMapper chapterMapper;

    public ChapterService(ChapterMapper chapterMapper) {
        this.chapterMapper = chapterMapper;
    }

    /** Persists a generated chapter in its own transaction (AI call is outside tx). */
    @Transactional
    public Chapter saveChapter(Chapter chapter) {
        if (chapter.getGenerationStatus() == null) {
            chapter.setGenerationStatus("GENERATED");
        }
        if (chapter.getId() == null) {
            chapterMapper.insert(chapter);
        } else {
            // TASK-123/124: re-save after extraction to checkpoint
            // memory_extraction_status (PENDING -> COMPLETED/FAILED) without
            // re-inserting the row.
            chapterMapper.update(chapter);
        }
        return chapterMapper.findById(chapter.getId());
    }

    /** Loads a chapter by id or throws 404. */
    public Chapter getChapter(Long id) {
        Chapter c = chapterMapper.findById(id);
        if (c == null) {
            throw new ResourceNotFoundException("Chapter", id);
        }
        return c;
    }

    public List<Chapter> listByStage(Long stageId) {
        return chapterMapper.findByStageId(stageId);
    }

    public List<Chapter> listByStory(Long storyId) {
        return chapterMapper.findByStoryId(storyId);
    }

    /** Next sequential chapter number for the story (max + 1, minimum 1). */
    public int nextChapterNumber(Long storyId) {
        return chapterMapper.maxChapterNumber(storyId) + 1;
    }
}
