package com.example.storyai.chapter.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.chapter.mapper.ChapterRevisionMapper;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.chapter.model.ChapterRevision;
import com.example.storyai.common.exception.ResourceNotFoundException;

/**
 * Revision history + author approval lifecycle (v0.1.1 Phase 5, TASK-140..146).
 *
 * <p>Core rule: <b>generated/edited text is a draft until the author accepts it</b>.
 * Every write creates an immutable {@link ChapterRevision}; the chapter row keeps
 * pointing at the revision it currently exposes. Nothing is ever overwritten in
 * place — history is the product.</p>
 */
@Service
public class ChapterRevisionService {

    private final ChapterRevisionMapper revisionMapper;
    private final ChapterService chapterService;

    public ChapterRevisionService(ChapterRevisionMapper revisionMapper,
                                  ChapterService chapterService) {
        this.revisionMapper = revisionMapper;
        this.chapterService = chapterService;
    }

    /** All revisions of a chapter, newest first. */
    public List<ChapterRevision> listRevisions(Long chapterId) {
        chapterService.getChapter(chapterId); // 404 when unknown
        return revisionMapper.findByChapterId(chapterId);
    }

    public ChapterRevision getRevision(Long revisionId) {
        ChapterRevision r = revisionMapper.findById(revisionId);
        if (r == null) {
            throw new ResourceNotFoundException("ChapterRevision", revisionId);
        }
        return r;
    }

    /**
     * Creates a new immutable revision from the given content and makes it the
     * chapter's current one. The author-facing status stays/re-enters DRAFT —
     * only an explicit approve flips it to APPROVED.
     *
     * @param sourceType AI_GENERATED / MANUAL_EDIT / AI_REWRITE / AI_POLISH
     * @return the persisted revision
     */
    @Transactional
    public ChapterRevision createRevision(Long chapterId, String content, String sourceType) {
        Chapter chapter = chapterService.getChapter(chapterId);
        Integer maxVersion = revisionMapper.findMaxVersion(chapterId);

        ChapterRevision revision = new ChapterRevision();
        revision.setChapterId(chapterId);
        revision.setVersionNumber((maxVersion == null ? 0 : maxVersion) + 1);
        revision.setContent(content);
        revision.setSourceType(sourceType);
        revisionMapper.insert(revision);

        // Keep the denormalized read model in sync; a new write re-opens DRAFT.
        chapter.setContent(content);
        chapter.setCurrentRevisionId(revision.getId());
        chapter.setStatus("DRAFT");
        chapterService.saveChapter(chapter);
        return revision;
    }

    /** TASK-146 — author accepts the current revision: DRAFT → APPROVED. */
    @Transactional
    public Chapter approve(Long chapterId) {
        Chapter chapter = chapterService.getChapter(chapterId);
        if ("APPROVED".equals(chapter.getStatus())) {
            return chapter; // idempotent
        }
        chapter.setStatus("APPROVED");
        return chapterService.saveChapter(chapter);
    }
}
