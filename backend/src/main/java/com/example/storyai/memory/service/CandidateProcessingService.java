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
        // TASK-161: only the frozen type enum may be auto-applied. An unknown
        // type is a contract violation from the extractor — it must never be
        // silently stored as a long-term story fact; route it to REVIEW.
        if (!com.example.storyai.memory.model.MemoryTypes.ALL.contains(candidate.getType())) {
            log.warn("Unknown memory type '{}' from candidate {} -> REVIEW",
                    candidate.getType(), candidate.getId());
            memoryService.updateCandidateStatus(candidate.getId(), "PENDING", false);
            return;
        }
        // TASK-165 (AC-105 bread-loop guard): inventory slots are CUMULATIVE —
        // an AUTO-applied trifle would sit in Current State forever and leak
        // into every later writer context. Structural rule, not a word list:
        // an item:* slot needs importance >= 4 to be applied without the author.
        if (candidate.getField() != null && candidate.getField().startsWith("item:")
                && candidate.getImportance() < 4) {
            log.info("Inventory candidate {} importance {} < 4 -> REVIEW (field={}, type={})",
                    candidate.getId(), candidate.getImportance(), candidate.getField(), candidate.getType());
            memoryService.updateCandidateStatus(candidate.getId(), "PENDING", false);
            return;
        }
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
        // TASK-163: inventory slots are per-item so multiple possessions coexist.
        // field="inventory"/"item"/... + value="铁剑" becomes field="item:铁剑";
        // losing the sword later deletes only that slot, never the whole inventory.
        String effectiveField = normalizeInventoryField(field, c.getValue());
        CurrentState s = new CurrentState();
        s.setStoryId(c.getStoryId());
        s.setCategory(categoryFor(field));
        s.setSubject(c.getSubject());
        s.setField(effectiveField);
        s.setValue(c.getValue());
        s.setSourceCandidateId(c.getId());
        memoryService.upsertCurrentState(s);
        log.info("Applied CURRENT_STATE candidate {} -> {}:{}", c.getId(), effectiveField, c.getValue());
    }

    /** TASK-163 — one slot per item: item:<normalized item name>. */
    private String normalizeInventoryField(String field, String value) {
        String f = field.toLowerCase();
        boolean inventoryLike = f.equals("inventory") || f.equals("item")
                || f.equals("weapon") || f.equals("equipment");
        if (!inventoryLike) {
            return field;
        }
        String item = value == null ? "" : value.trim();
        if (item.isEmpty()) {
            return field;
        }
        // keep a normalized, bounded key: collapse whitespace, cap length
        item = item.replaceAll("\\s+", "");
        if (item.length() > 48) {
            item = item.substring(0, 48);
        }
        return "item:" + item;
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
        r.setSourceCandidateId(c.getId());
        memoryService.upsertRelationship(r);
        log.info("Applied RELATIONSHIP candidate {} -> {} / {}", c.getId(), r.getSubjectA(), r.getSubjectB());
    }

    private void applyStoryMemory(MemoryCandidate c) {
        // TASK-162 — Dedup v1: normalized exact match against active rows with
        // the same (story, type, subject). A duplicate refreshes nothing and
        // inserts nothing; the existing fact already covers it. No embeddings.
        String type = c.getType();
        List<StoryMemory> sameSubject = memoryService.findActiveByTypeSubject(
                c.getStoryId(), type, c.getSubject());
        if (isDuplicate(sameSubject, c.getValue())) {
            memoryService.updateCandidateStatus(c.getId(), "APPLIED", false);
            log.info("Dedup: candidate {} matches an existing {} memory for '{}' — not inserted",
                    c.getId(), type, c.getSubject());
            return;
        }

        StoryMemory m = new StoryMemory();
        m.setStoryId(c.getStoryId());
        m.setType(type);
        m.setSubject(c.getSubject());
        m.setDescription(c.getValue());
        m.setSourceChapterId(c.getSourceChapterId());
        m.setEvidence(c.getEvidence());
        // TASK-159: clamp/normalize at the storage boundary — every path into
        // story_memory goes through here, so invalid values can never persist.
        m.setImportance(com.example.storyai.memory.model.MemoryTypes.clampImportance(c.getImportance()));
        m.setScope(com.example.storyai.memory.model.MemoryTypes.normalizeScope(c.getScope()));
        m.setActive(true);
        memoryService.saveStoryMemory(m);
        log.info("Applied STORY_MEMORY candidate {} -> type {}", c.getId(), type);
    }

    /** TASK-162 — normalized exact description match (whitespace-collapsed). */
    private boolean isDuplicate(List<StoryMemory> candidates, String newValue) {
        if (newValue == null || newValue.isBlank()) {
            return false;
        }
        String normalizedNew = normalizeForDedup(newValue);
        for (StoryMemory existing : candidates) {
            if (existing.getDescription() == null) {
                continue;
            }
            if (normalizeForDedup(existing.getDescription()).equals(normalizedNew)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForDedup(String text) {
        return text.replaceAll("\\s+", "").trim();
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
     * candidates created. Each candidate is reverse-mapped with the same slot
     * normalization used by Apply, but deletion succeeds only while that
     * candidate is still recorded as the live slot's source. A newer chapter's
     * upsert therefore cannot be erased by revising an older chapter.
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
                    String effectiveField = normalizeInventoryField(field, c.getValue());
                    removed += memoryService.deleteCurrentStateSlotIfSource(
                            c.getStoryId(),
                            categoryFor(field),
                            c.getSubject(),
                            effectiveField,
                            c.getId());
                }
            } else if ("RELATIONSHIP".equals(c.getType())) {
                String subj = c.getSubject() == null ? "" : c.getSubject();
                int arrow = subj.indexOf("->");
                String a = arrow > 0 ? subj.substring(0, arrow).trim() : subj.trim();
                String b = arrow > 0 ? subj.substring(arrow + 2).trim() : "(story)";
                removed += memoryService.deleteRelationshipSlotIfSource(
                        c.getStoryId(), a, b, c.getId());
            }
            // audit trail: never re-apply an invalidated candidate
            memoryService.updateCandidateStatus(c.getId(), "SUPERSEDED", false);
        }
        log.info("Invalidated {} live-memory slot(s) derived from chapter {}", removed, chapterId);
        return removed;
    }
}
