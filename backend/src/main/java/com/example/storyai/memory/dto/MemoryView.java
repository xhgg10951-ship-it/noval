package com.example.storyai.memory.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only view of a story's memory state for the review UI (TASK-034/035).
 */
public class MemoryView {

    private List<CandidateDto> candidates;
    private List<StateDto> currentState;
    private List<RelationshipDto> relationships;
    private List<StoryMemoryDto> storyMemories;

    public MemoryView() {
    }

    public MemoryView(List<CandidateDto> candidates, List<StateDto> currentState,
                      List<RelationshipDto> relationships, List<StoryMemoryDto> storyMemories) {
        this.candidates = candidates;
        this.currentState = currentState;
        this.relationships = relationships;
        this.storyMemories = storyMemories;
    }

    public List<CandidateDto> getCandidates() { return candidates; }
    public void setCandidates(List<CandidateDto> candidates) { this.candidates = candidates; }

    public List<StateDto> getCurrentState() { return currentState; }
    public void setCurrentState(List<StateDto> currentState) { this.currentState = currentState; }

    public List<RelationshipDto> getRelationships() { return relationships; }
    public void setRelationships(List<RelationshipDto> relationships) { this.relationships = relationships; }

    public List<StoryMemoryDto> getStoryMemories() { return storyMemories; }
    public void setStoryMemories(List<StoryMemoryDto> storyMemories) { this.storyMemories = storyMemories; }

    public static class CandidateDto {
        public Long id;
        public Long storyId;
        public Long sourceChapterId;
        public String type;
        public String subject;
        public String field;
        public String value;
        public String suggestedAction;
        public String evidence;
        public String processingStatus;
        public boolean applied;
        public LocalDateTime createdAt;
    }

    public static class StateDto {
        public Long id;
        public String category;
        public String subject;
        public String field;
        public String value;
    }

    public static class RelationshipDto {
        public Long id;
        public String subjectA;
        public String subjectB;
        public String description;
    }

    public static class StoryMemoryDto {
        public Long id;
        public String type;
        public String subject;
        public String description;
        public int importance;
        public String scope;
        public boolean active;
        public Long sourceChapterId;
        public String evidence;
    }
}
