package com.example.storyai.arc.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.arc.mapper.ArcMapper;
import com.example.storyai.arc.model.Arc;
import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.story.service.StoryService;

/**
 * Arc persistence + minimal lifecycle (v0.1.1 Phase 6 / TASK-152).
 *
 * <p>Deliberately simple: create / list / update / current. No arc versioning,
 * no timeline UI. Ranges must not overlap within a story so "current arc by
 * chapter number" stays unambiguous.</p>
 */
@Service
public class ArcService {

    private final ArcMapper arcMapper;
    private final StoryService storyService;

    public ArcService(ArcMapper arcMapper, StoryService storyService) {
        this.arcMapper = arcMapper;
        this.storyService = storyService;
    }

    @Transactional
    public Arc create(Long storyId, Arc arc) {
        storyService.getStory(storyId); // 404 guard
        validateRange(arc.getTargetStartChapter(), arc.getTargetEndChapter());
        ensureNoOverlap(storyId, null, arc.getTargetStartChapter(), arc.getTargetEndChapter());
        if (Arc.STATUS_ACTIVE.equals(arc.getStatus())) {
            clearActive(storyId);
        }
        arc.setStoryId(storyId);
        arcMapper.insert(arc);
        return arcMapper.findById(arc.getId());
    }

    public Arc get(Long id) {
        Arc arc = arcMapper.findById(id);
        if (arc == null) {
            throw new ResourceNotFoundException("Arc", id);
        }
        return arc;
    }

    public List<Arc> listByStory(Long storyId) {
        storyService.getStory(storyId);
        return arcMapper.findByStoryId(storyId);
    }

    /** The arc covering the given chapter number; falls back to the ACTIVE arc.
     *  Used by context assembly (the Planner always wants SOME arc context). */
    public Arc findCurrent(Long storyId, int chapterNumber) {
        Arc byRange = arcMapper.findCurrentByChapter(storyId, chapterNumber);
        return byRange != null ? byRange : arcMapper.findActive(storyId);
    }

    /** TASK-152 API: strict by-range lookup — no ACTIVE fallback, 404 when outside. */
    public Arc findByChapterStrict(Long storyId, int chapterNumber) {
        return arcMapper.findCurrentByChapter(storyId, chapterNumber);
    }

    @Transactional
    public Arc update(Long id, Arc input) {
        Arc existing = get(id);
        Integer start = input.getTargetStartChapter() != null
                ? input.getTargetStartChapter() : existing.getTargetStartChapter();
        Integer end = input.getTargetEndChapter() != null
                ? input.getTargetEndChapter() : existing.getTargetEndChapter();
        validateRange(start, end);
        ensureNoOverlap(existing.getStoryId(), id, start, end);

        existing.setTitle(input.getTitle() != null ? input.getTitle() : existing.getTitle());
        existing.setGoal(input.getGoal() != null ? input.getGoal() : existing.getGoal());
        existing.setStatus(input.getStatus() != null ? input.getStatus() : existing.getStatus());
        existing.setTargetStartChapter(start);
        existing.setTargetEndChapter(end);
        if (Arc.STATUS_ACTIVE.equals(existing.getStatus())) {
            clearActive(existing.getStoryId());
        }
        arcMapper.update(existing);
        return arcMapper.findById(id);
    }

    private void validateRange(Integer start, Integer end) {
        if (start == null || end == null || start < 1 || end < start) {
            throw new IllegalArgumentException("Arc 章节范围无效（需 1 ≤ 起始 ≤ 结束）");
        }
    }

    /** Arc ranges within a story must not overlap ("current arc" must be unique). */
    private void ensureNoOverlap(Long storyId, Long excludeId, int start, int end) {
        for (Arc other : arcMapper.findByStoryId(storyId)) {
            if (other.getId().equals(excludeId)) {
                continue;
            }
            boolean overlaps = start <= other.getTargetEndChapter()
                    && end >= other.getTargetStartChapter();
            if (overlaps) {
                throw new IllegalArgumentException(String.format(
                        "章节范围与已有卷《%s》（第%d–%d章）重叠", 
                        other.getTitle(), other.getTargetStartChapter(), other.getTargetEndChapter()));
            }
        }
    }

    private void clearActive(Long storyId) {
        Arc active = arcMapper.findActive(storyId);
        if (active != null) {
            arcMapper.updateStatus(active.getId(), Arc.STATUS_PLANNED);
        }
    }
}
