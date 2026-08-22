package com.example.storyai.memory.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;

/** MyBatis mapper for the M4 memory tables (TASK-026). */
@Mapper
public interface MemoryMapper {

    // ---- memory_candidate ----
    int insertCandidate(MemoryCandidate c);

    List<MemoryCandidate> findCandidatesByStory(@Param("storyId") Long storyId);

    List<MemoryCandidate> findPendingCandidates(@Param("storyId") Long storyId);

    MemoryCandidate findCandidateById(@Param("id") Long id);

    void updateCandidateStatus(@Param("id") Long id,
                               @Param("processingStatus") String processingStatus,
                               @Param("applied") boolean applied);

    // ---- current_state ----
    void upsertCurrentState(CurrentState s);

    List<CurrentState> findCurrentState(@Param("storyId") Long storyId);

    // ---- relationship_state ----
    void upsertRelationship(RelationshipState r);

    List<RelationshipState> findRelationships(@Param("storyId") Long storyId);

    // ---- story_memory ----
    int insertStoryMemory(StoryMemory m);

    List<StoryMemory> findStoryMemories(@Param("storyId") Long storyId);

    // ---- TASK-148: invalidate a chapter's derived memories on revision change ----

    /** Deletes the STORY_MEMORY rows derived from one chapter (source-tracked). */
    int deleteStoryMemoriesBySource(@Param("chapterId") Long chapterId);

    /** Marks the chapter's old extraction candidates SUPERSEDED (audit trail kept). */
    int supersedeCandidatesBySource(@Param("chapterId") Long chapterId);
}
