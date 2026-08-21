package com.example.storyai.chapter.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.chapter.dto.ChapterResponse;
import com.example.storyai.chapter.service.ChapterGenerationService;
import com.example.storyai.chapter.service.ChapterService;

/**
 * REST API for single-chapter generation + reading (TASK-023/024, AT-C01).
 *
 * <pre>
 * POST /api/stages/{stageId}/chapters   -> generate next pending chapter (200 / 404 / 409 / 502)
 * GET  /api/stages/{stageId}/chapters   -> list chapters for the stage (200)
 * GET  /api/chapters/{chapterId}         -> read one chapter (200 / 404)
 * </pre>
 */
@RestController
public class ChapterController {

    private final ChapterGenerationService generationService;
    private final ChapterService chapterService;

    public ChapterController(ChapterGenerationService generationService,
                             ChapterService chapterService) {
        this.generationService = generationService;
        this.chapterService = chapterService;
    }

    /** AT-C01: generate the next pending chapter for a stage. */
    @PostMapping("/api/stages/{stageId}/chapters")
    public ChapterResponse generateNext(@PathVariable Long stageId) {
        return new ChapterResponse(generationService.generateNextChapter(stageId));
    }

    @GetMapping("/api/stages/{stageId}/chapters")
    public List<ChapterResponse> listStageChapters(@PathVariable Long stageId) {
        return chapterService.listByStage(stageId).stream()
                .map(ChapterResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/chapters/{chapterId}")
    public ChapterResponse getChapter(@PathVariable Long chapterId) {
        return new ChapterResponse(chapterService.getChapter(chapterId));
    }
}
