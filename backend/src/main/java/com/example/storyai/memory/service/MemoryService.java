package com.example.storyai.memory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.memory.mapper.MemoryMapper;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;

/**
 * Transactional persistence + read for the M4 memory tables (TASK-026).
 *
 * <p>All writes run in a transaction. The AI/HTTP extraction call lives in
 * {@link MemoryExtractionService} and is deliberately kept OUTSIDE any
 * transaction here.</p>
 */
@Service
public class MemoryService {

    private final MemoryMapper memoryMapper;

    public MemoryService(MemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    // ---- candidates ----
    @Transactional
    public MemoryCandidate saveCandidate(MemoryCandidate c) {
        memoryMapper.insertCandidate(c);
        return memoryMapper.findCandidateById(c.getId());
    }

    @Transactional
    public void updateCandidateStatus(Long id, String processingStatus, boolean applied) {
        memoryMapper.updateCandidateStatus(id, processingStatus, applied);
    }

    public List<MemoryCandidate> listCandidates(Long storyId) {
        return memoryMapper.findCandidatesByStory(storyId);
    }

    public List<MemoryCandidate> listPending(Long storyId) {
        return memoryMapper.findPendingCandidates(storyId);
    }

    /** TASK-148: the chapter's APPLIED candidates (slot reverse-lookup source). */
    public List<MemoryCandidate> listAppliedBySource(Long chapterId) {
        return memoryMapper.findAppliedCandidatesBySource(chapterId);
    }

    @Transactional
    public int supersedeCandidatesBySource(Long chapterId) {
        return memoryMapper.supersedeCandidatesBySource(chapterId);
    }

    /** TASK-148: removes one exact current-state slot. */
    @Transactional
    public int deleteCurrentStateSlot(Long storyId, String category, String subject, String field) {
        return memoryMapper.deleteCurrentStateSlot(storyId, category, subject, field);
    }

    /** RH-02: revision invalidation cannot delete a slot written by a newer candidate. */
    @Transactional
    public int deleteCurrentStateSlotIfSource(Long storyId,
                                              String category,
                                              String subject,
                                              String field,
                                              Long sourceCandidateId) {
        return memoryMapper.deleteCurrentStateSlotIfSource(
                storyId, category, subject, field, sourceCandidateId);
    }

    /** TASK-148: removes one exact relationship slot. */
    @Transactional
    public int deleteRelationshipSlot(Long storyId, String subjectA, String subjectB) {
        return memoryMapper.deleteRelationshipSlot(storyId, subjectA, subjectB);
    }

    /** RH-02: revision invalidation cannot delete a relationship written later. */
    @Transactional
    public int deleteRelationshipSlotIfSource(Long storyId,
                                              String subjectA,
                                              String subjectB,
                                              Long sourceCandidateId) {
        return memoryMapper.deleteRelationshipSlotIfSource(
                storyId, subjectA, subjectB, sourceCandidateId);
    }

    public MemoryCandidate getCandidate(Long id) {
        return memoryMapper.findCandidateById(id);
    }

    // ---- current state (upsert by slot) ----
    @Transactional
    public void upsertCurrentState(CurrentState s) {
        memoryMapper.upsertCurrentState(s);
    }

    public List<CurrentState> getCurrentState(Long storyId) {
        return memoryMapper.findCurrentState(storyId);
    }

    // ---- relationships (upsert by pair) ----
    @Transactional
    public void upsertRelationship(RelationshipState r) {
        memoryMapper.upsertRelationship(r);
    }

    public List<RelationshipState> getRelationships(Long storyId) {
        return memoryMapper.findRelationships(storyId);
    }

    // ---- story memory ----
    @Transactional
    public StoryMemory saveStoryMemory(StoryMemory m) {
        memoryMapper.insertStoryMemory(m);
        return m;
    }

    public List<StoryMemory> getStoryMemories(Long storyId) {
        return memoryMapper.findStoryMemories(storyId);
    }

    /** TASK-162: dedup candidate set (same story+type+subject, still active). */
    public List<StoryMemory> findActiveByTypeSubject(Long storyId, String type, String subject) {
        return memoryMapper.findActiveByTypeSubject(storyId, type, subject);
    }
}
