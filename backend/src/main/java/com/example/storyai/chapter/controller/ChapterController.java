package com.example.storyai.chapter.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.chapter.dto.ChapterResponse;
import com.example.storyai.chapter.dto.ChapterRevisionResponse;
import com.example.storyai.chapter.dto.EditChapterContentRequest;
import com.example.storyai.chapter.model.ChapterRevision;
import com.example.storyai.chapter.service.ChapterGenerationService;
import com.example.storyai.chapter.service.ChapterRevisionService;
import com.example.storyai.chapter.service.ChapterService;

/**
 * REST API for single-chapter generation + reading (TASK-023/024, AT-C01)
 * and the v0.1.1 Phase 5 author workflow (TASK-142..146).
 *
 * <pre>
 * POST /api/stages/{stageId}/chapters        -> generate next pending chapter
 * GET  /api/stages/{stageId}/chapters        -> list chapters for the stage
 * GET  /api/chapters/{chapterId}             -> read one chapter
 * GET  /api/chapters/{chapterId}/revisions   -> revision history, newest first (TASK-145)
 * PUT  /api/chapters/{chapterId}/content     -> manual edit = NEW revision (TASK-142)
 * POST /api/chapters/{chapterId}/approve     -> DRAFT -> APPROVED (TASK-146)
 * </pre>
 */
@RestController
public class ChapterController {

    private final ChapterGenerationService generationService;
    private final ChapterService chapterService;
    private final ChapterRevisionService revisionService;

    public ChapterController(ChapterGenerationService generationService,
                             ChapterService chapterService,
                             ChapterRevisionService revisionService) {
        this.generationService = generationService;
        this.chapterService = chapterService;
        this.revisionService = revisionService;
    }

    /** AT-C01: generate the next pending chapter for a stage. */
    @PostMapping("/api/stages/{stageId}/chapters")
    public ChapterResponse generateNext(@PathVariable Long stageId) {
        return withRevisionVersion(new ChapterResponse(generationService.generateNextChapter(stageId)));
    }

    @GetMapping("/api/stages/{stageId}/chapters")
    public List<ChapterResponse> listStageChapters(@PathVariable Long stageId) {
        return chapterService.listByStage(stageId).stream()
                .map(ChapterResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/chapters/{chapterId}")
    public ChapterResponse getChapter(@PathVariable Long chapterId) {
        return withRevisionVersion(new ChapterResponse(chapterService.getChapter(chapterId)));
    }

    // ---- v0.1.1 Phase 5: author workflow over immutable revisions ----

    /** TASK-145 — full history of a chapter, newest first. */
    @GetMapping("/api/chapters/{chapterId}/revisions")
    public List<ChapterRevisionResponse> listRevisions(@PathVariable Long chapterId) {
        return revisionService.listRevisions(chapterId).stream()
                .map(ChapterRevisionResponse::new)
                .collect(Collectors.toList());
    }

    /** TASK-142 — author saves an edit; it becomes a NEW MANUAL_EDIT revision. */
    @PutMapping("/api/chapters/{chapterId}/content")
    public ChapterResponse editContent(@PathVariable Long chapterId,
                                       @Valid @RequestBody EditChapterContentRequest request) {
        revisionService.createRevision(chapterId, request.getContent(),
                ChapterRevision.SOURCE_MANUAL_EDIT);
        return withRevisionVersion(new ChapterResponse(chapterService.getChapter(chapterId)));
    }

    /** TASK-146 — author accepts the current revision: DRAFT → APPROVED. */
    @PostMapping("/api/chapters/{chapterId}/approve")
    public ChapterResponse approve(@PathVariable Long chapterId) {
        return withRevisionVersion(new ChapterResponse(revisionService.approve(chapterId)));
    }

    /** TASK-144 — regenerate the same chapter from its same ChapterSpec. */
    @PostMapping("/api/chapters/{chapterId}/regenerate")
    public ChapterResponse regenerate(@PathVariable Long chapterId,
                                      @org.springframework.web.bind.annotation.RequestBody(
                                              required = false)
                                      java.util.Map<String, String> body) {
        String instruction = body == null ? null : body.get("authorInstruction");
        return withRevisionVersion(
                new ChapterResponse(generationService.regenerateChapter(chapterId, instruction)));
    }

    private ChapterResponse withRevisionVersion(ChapterResponse response) {
        if (response.getCurrentRevisionId() != null) {
            ChapterRevision current = revisionService.getRevision(response.getCurrentRevisionId());
            response.setCurrentRevisionVersion(current.getVersionNumber());
            response.setSourceType(current.getSourceType());
        }
        return response;
    }
}
