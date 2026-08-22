package com.example.storyai.memory.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;

/**
 * Decides the fate of memory candidates (TASK-029, AT-H01/H02/H03).
 *
 * <pre>
 * AUTO   -> safe-apply to CurrentState / StoryMemory, mark APPLIED
 * REVIEW -> leave PENDING for the author (no silent apply)
 * IGNORE -> mark IGNORED
 * </pre>
 *
 * <p>Author override is supported through {@link #applyCandidate} /
 * {@link #ignoreCandidate} so the UI can force AUTO->IGNORE or IGNORE->AUTO.</p>
 *
 * <p>Architecture guard: only specific candidate types may mutate CurrentState
 * automatically. A candidate labelled {@code CURRENT_STATE} with a recognised
 * {@code field} (location / inventory / physical_condition / emotion /
 * current_goal) updates the matching slot; anything else (relationship nuances,
 * world rules, foreshadowing) is routed to StoryMemory or left for REVIEW, never
 * blindly written into CurrentState.</p>
 */
@Service
public class CandidateProcessingService {

    private static final Logger log = LoggerFactory.getLogger(CandidateProcessingService.class);

    private final MemoryService memoryService;

    public CandidateProcessingService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /** Applies a candidate exactly according to its suggestedAction (used after extraction). */
    public void autoProcess(MemoryCandidate candidate) {
        String action = candidate.getSuggestedAction();
        if ("AUTO".equals(action)) {
            applyCandidate(candidate);
        } else if ("IGNORE".equals(action)) {
            memoryService.updateCandidateStatus(candidate.getId(), "IGNORED", false);
        } else {
            // REVIEW: stay PENDING, wait for the author
            memoryService.updateCandidateStatus(candidate.getId(), "PENDING", false);
        }
    }

    /** Author (or explicit override) accepts a candidate and applies it to live memory. */
    public void applyCandidate(MemoryCandidate candidate) {
        String type = candidate.getType();
        if ("CURRENT_STATE".equals(type)) {
            applyCurrentState(candidate);
        } else if ("RELATIONSHIP".equals(type)) {
            applyRelationship(candidate);
        } else {
            // STORY_MEMORY / DETAIL / EVENT / FORESHADOW / SECRET ... -> long-term memory
            applyStoryMemory(candidate);
        }
        memoryService.updateCandidateStatus(candidate.getId(), "APPLIED", true);
    }

    /** Author ignores a candidate. */
    public void ignoreCandidate(MemoryCandidate candidate) {
        memoryService.updateCandidateStatus(candidate.getId(), "IGNORED", false);
    }

    private void applyCurrentState(MemoryCandidate c) {
        String field = c.getField();
        if (field == null || field.isBlank()) {
            // No concrete slot -> treat as a detail memory instead of a state slot.
            applyStoryMemory(c);
            return;
        }
        CurrentState s = new CurrentState();
        s.setStoryId(c.getStoryId());
        s.setCategory(categoryFor(field));
        s.setSubject(c.getSubject());
        s.setField(field);
        s.setValue(c.getValue());
        memoryService.upsertCurrentState(s);
        log.info("Applied CURRENT_STATE candidate {} -> {}:{}", c.getId(), field, c.getValue());
    }

    private void applyRelationship(MemoryCandidate c) {
        RelationshipState r = new RelationshipState();
        r.setStoryId(c.getStoryId());
        // subject may be "A->B" or "A"; split if possible.
        String subj = c.getSubject() == null ? "" : c.getSubject();
        int arrow = subj.indexOf("->");
        if (arrow > 0) {
            r.setSubjectA(subj.substring(0, arrow).trim());
            r.setSubjectB(subj.substring(arrow + 2).trim());
        } else {
            r.setSubjectA(subj.trim());
            r.setSubjectB("(story)");
        }
        r.setDescription(c.getValue());
        memoryService.upsertRelationship(r);
        log.info("Applied RELATIONSHIP candidate {} -> {} / {}", c.getId(), r.getSubjectA(), r.getSubjectB());
    }

    private void applyStoryMemory(MemoryCandidate c) {
        StoryMemory m = new StoryMemory();
        m.setStoryId(c.getStoryId());
        m.setType(c.getType());
        m.setSubject(c.getSubject());
        m.setDescription(c.getValue());
        m.setSourceChapterId(c.getSourceChapterId());
        m.setEvidence(c.getEvidence());
        memoryService.saveStoryMemory(m);
        log.info("Applied STORY_MEMORY candidate {} -> type {}", c.getId(), c.getType());
    }

    /** Maps a state field to a coarse category for CurrentState grouping. */
    private String categoryFor(String field) {
        return switch (field.toLowerCase()) {
            case "location", "place" -> "LOCATION";
            case "inventory", "item", "weapon", "equipment" -> "INVENTORY";
            case "physical_condition", "injury", "health" -> "PHYSICAL_CONDITION";
            case "emotion", "mood" -> "EMOTION";
            case "current_goal", "goal" -> "CURRENT_GOAL";
            default -> "OTHER";
        };
    }

    public List<MemoryCandidate> pending(Long storyId) {
        return memoryService.listPending(storyId);
    }

    /**
     * TASK-148 — invalidates the live-memory slots this chapter's APPLIED
     * candidates created. The candidate rows are the only per-chapter provenance
     * for current_state / relationship_state (those tables carry no source
     * column), so each applied candidate is reverse-mapped back to its exact
     * slot using the SAME field->category mapping the apply path used, and that
     * slot is deleted. STORY_MEMORY rows are source-tracked and are deleted by
     * {@code deleteStoryMemoriesBySource} at the call site.
     *
     * @return number of live slots removed
     */
    @org.springframework.transaction.annotation.Transactional
    public int invalidateAppliedSlots(Long chapterId) {
        List<MemoryCandidate> applied = memoryService.listAppliedBySource(chapterId);
        int removed = 0;
        for (MemoryCandidate c : applied) {
            if ("CURRENT_STATE".equals(c.getType())) {
                String field = c.getField();
                if (field != null && !field.isBlank()) {
                    removed += memoryService.deleteCurrentStateSlot(
                            c.getStoryId(), categoryFor(field), c.getSubject(), field);
                }
            } else if ("RELATIONSHIP".equals(c.getType())) {
                String subj = c.getSubject() == null ? "" : c.getSubject();
                int arrow = subj.indexOf("->");
                String a = arrow > 0 ? subj.substring(0, arrow).trim() : subj.trim();
                String b = arrow > 0 ? subj.substring(arrow + 2).trim() : "(story)";
                removed += memoryService.deleteRelationshipSlot(c.getStoryId(), a, b);
            }
            // audit trail: never re-apply an invalidated candidate
            memoryService.updateCandidateStatus(c.getId(), "SUPERSEDED", false);
        }
        log.info("Invalidated {} live-memory slot(s) derived from chapter {}", removed, chapterId);
        return removed;
    }
}
