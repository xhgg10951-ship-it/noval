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

    /** TASK-148: the chapter's APPLIED candidates (slot reverse-lookup source). */
    List<MemoryCandidate> findAppliedCandidatesBySource(@Param("chapterId") Long chapterId);

    /** Revision invalidation applies to every candidate from the obsolete extraction. */
    int supersedeCandidatesBySource(@Param("chapterId") Long chapterId);

    // ---- current_state ----
    void upsertCurrentState(CurrentState s);

    List<CurrentState> findCurrentState(@Param("storyId") Long storyId);

    /** TASK-148: removes one exact slot (reverse-derived from an APPLIED candidate). */
    int deleteCurrentStateSlot(@Param("storyId") Long storyId,
                               @Param("category") String category,
                               @Param("subject") String subject,
                               @Param("field") String field);

    /** RH-02: removes a slot only when the invalidated candidate still owns it. */
    int deleteCurrentStateSlotIfSource(@Param("storyId") Long storyId,
                                       @Param("category") String category,
                                       @Param("subject") String subject,
                                       @Param("field") String field,
                                       @Param("sourceCandidateId") Long sourceCandidateId);

    // ---- relationship_state ----
    void upsertRelationship(RelationshipState r);

    List<RelationshipState> findRelationships(@Param("storyId") Long storyId);

    /** TASK-148: removes one exact relationship slot. */
    int deleteRelationshipSlot(@Param("storyId") Long storyId,
                               @Param("subjectA") String subjectA,
                               @Param("subjectB") String subjectB);

    /** RH-02: removes a relationship only when the invalidated candidate owns it. */
    int deleteRelationshipSlotIfSource(@Param("storyId") Long storyId,
                                       @Param("subjectA") String subjectA,
                                       @Param("subjectB") String subjectB,
                                       @Param("sourceCandidateId") Long sourceCandidateId);

    // ---- story_memory ----
    int insertStoryMemory(StoryMemory m);

    List<StoryMemory> findStoryMemories(@Param("storyId") Long storyId);

    /** TASK-162: active rows with the same type+subject — the dedup candidate set. */
    List<StoryMemory> findActiveByTypeSubject(@Param("storyId") Long storyId,
                                              @Param("type") String type,
                                              @Param("subject") String subject);

    // ---- TASK-148: invalidate a chapter's derived memories on revision change ----

    /** Deletes the STORY_MEMORY rows derived from one chapter (source-tracked). */
    int deleteStoryMemoriesBySource(@Param("chapterId") Long chapterId);
}
