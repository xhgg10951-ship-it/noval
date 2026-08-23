package com.example.storyai.memory.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.memory.dto.MemoryView;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.service.CandidateProcessingService;
import com.example.storyai.memory.service.MemoryService;

/**
 * REST API for memory review + author control (TASK-034, AT-H01/H02/H03).
 *
 * <pre>
 * GET  /api/stories/{storyId}/memory            -> full memory view (AT-I01)
 * GET  /api/stories/{storyId}/memory/candidates  -> candidates (incl. pending)
 * POST /api/memory/candidates/{id}/apply          -> author accepts (AUTO/REVIEW->apply)
 * POST /api/memory/candidates/{id}/ignore         -> author ignores
 * </pre>
 */
@RestController
public class MemoryController {

    private final MemoryService memoryService;
    private final CandidateProcessingService processingService;

    public MemoryController(MemoryService memoryService,
                            CandidateProcessingService processingService) {
        this.memoryService = memoryService;
        this.processingService = processingService;
    }

    @GetMapping("/api/stories/{storyId}/memory")
    public MemoryView getMemory(@PathVariable Long storyId) {
        MemoryView view = new MemoryView();
        view.setCandidates(toCandidateDtos(memoryService.listCandidates(storyId)));
        view.setCurrentState(memoryService.getCurrentState(storyId).stream().map(s -> {
            MemoryView.StateDto d = new MemoryView.StateDto();
            d.id = s.getId(); d.category = s.getCategory(); d.subject = s.getSubject();
            d.field = s.getField(); d.value = s.getValue();
            return d;
        }).collect(Collectors.toList()));
        view.setRelationships(memoryService.getRelationships(storyId).stream().map(r -> {
            MemoryView.RelationshipDto d = new MemoryView.RelationshipDto();
            d.id = r.getId(); d.subjectA = r.getSubjectA(); d.subjectB = r.getSubjectB();
            d.description = r.getDescription();
            return d;
        }).collect(Collectors.toList()));
        view.setStoryMemories(memoryService.getStoryMemories(storyId).stream().map(m -> {
            MemoryView.StoryMemoryDto d = new MemoryView.StoryMemoryDto();
            d.id = m.getId(); d.type = m.getType(); d.subject = m.getSubject();
            d.description = m.getDescription(); d.importance = m.getImportance();
            d.scope = m.getScope(); d.active = m.isActive();
            d.sourceChapterId = m.getSourceChapterId();
            d.evidence = m.getEvidence();
            return d;
        }).collect(Collectors.toList()));
        return view;
    }

    @GetMapping("/api/stories/{storyId}/memory/candidates")
    public List<MemoryView.CandidateDto> getCandidates(@PathVariable Long storyId) {
        return toCandidateDtos(memoryService.listCandidates(storyId));
    }

    /** Author accepts a candidate (overrides AUTO/REVIEW/IGNORE). */
    @PostMapping("/api/memory/candidates/{id}/apply")
    public MemoryView.CandidateDto apply(@PathVariable Long id) {
        MemoryCandidate c = requireCandidate(id);
        processingService.applyCandidate(c);
        return toCandidateDto(memoryService.getCandidate(id));
    }

    /** Author ignores a candidate (overrides AUTO/REVIEW). */
    @PostMapping("/api/memory/candidates/{id}/ignore")
    public MemoryView.CandidateDto ignore(@PathVariable Long id) {
        MemoryCandidate c = requireCandidate(id);
        processingService.ignoreCandidate(c);
        return toCandidateDto(memoryService.getCandidate(id));
    }

    private MemoryCandidate requireCandidate(Long id) {
        MemoryCandidate c = memoryService.getCandidate(id);
        if (c == null) throw new ResourceNotFoundException("MemoryCandidate", id);
        return c;
    }

    private List<MemoryView.CandidateDto> toCandidateDtos(List<MemoryCandidate> list) {
        return list.stream().map(this::toCandidateDto).collect(Collectors.toList());
    }

    private MemoryView.CandidateDto toCandidateDto(MemoryCandidate c) {
        MemoryView.CandidateDto d = new MemoryView.CandidateDto();
        d.id = c.getId();
        d.storyId = c.getStoryId();
        d.sourceChapterId = c.getSourceChapterId();
        d.type = c.getType();
        d.subject = c.getSubject();
        d.field = c.getField();
        d.value = c.getValue();
        d.suggestedAction = c.getSuggestedAction();
        d.evidence = c.getEvidence();
        d.importance = c.getImportance();
        d.scope = c.getScope();
        d.processingStatus = c.getProcessingStatus();
        d.applied = c.isApplied();
        d.createdAt = c.getCreatedAt();
        return d;
    }
}
