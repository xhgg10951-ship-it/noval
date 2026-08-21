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
}
