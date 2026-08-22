package com.example.storyai.arc.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.arc.dto.ArcRequest;
import com.example.storyai.arc.dto.ArcResponse;
import com.example.storyai.arc.model.Arc;
import com.example.storyai.arc.service.ArcService;

/**
 * REST API for arcs (v0.1.1 Phase 6 / TASK-152).
 *
 * <pre>
 * POST /api/stories/{storyId}/arcs                  -> create (201)
 * GET  /api/stories/{storyId}/arcs                  -> list (200)
 * GET  /api/stories/{storyId}/arcs/current?chapter=N-> current arc by chapter (200/404)
 * PUT  /api/arcs/{arcId}                            -> update (200)
 * </pre>
 */
@RestController
public class ArcController {

    private final ArcService arcService;

    public ArcController(ArcService arcService) {
        this.arcService = arcService;
    }

    @PostMapping("/api/stories/{storyId}/arcs")
    public ResponseEntity<ArcResponse> create(@PathVariable Long storyId,
                                              @Valid @RequestBody ArcRequest request) {
        Arc created = arcService.create(storyId, toArc(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ArcResponse(created));
    }

    @GetMapping("/api/stories/{storyId}/arcs")
    public List<ArcResponse> list(@PathVariable Long storyId) {
        return arcService.listByStory(storyId).stream()
                .map(ArcResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/arcs/{arcId}")
    public ArcResponse get(@PathVariable Long arcId) {
        return new ArcResponse(arcService.get(arcId));
    }

    @GetMapping("/api/stories/{storyId}/arcs/current")
    public ResponseEntity<ArcResponse> current(@PathVariable Long storyId,
                                               @RequestParam int chapter) {
        Arc current = arcService.findByChapterStrict(storyId, chapter);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ArcResponse(current));
    }

    @PutMapping("/api/arcs/{arcId}")
    public ArcResponse update(@PathVariable Long arcId, @Valid @RequestBody ArcRequest request) {
        return new ArcResponse(arcService.update(arcId, toArc(request)));
    }

    private Arc toArc(ArcRequest request) {
        Arc arc = new Arc();
        arc.setTitle(request.getTitle());
        arc.setGoal(request.getGoal());
        arc.setTargetStartChapter(request.getTargetStartChapter());
        arc.setTargetEndChapter(request.getTargetEndChapter());
        arc.setStatus(request.getStatus());
        return arc;
    }
}
