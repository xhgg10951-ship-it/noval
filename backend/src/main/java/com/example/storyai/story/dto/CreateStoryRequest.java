package com.example.storyai.story.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO for {@code POST /api/stories} (AT-A01).
 * Distinct from the DB model so the API boundary stays explicit (ARCHITECTURE §52).
 */
public class CreateStoryRequest {

    @NotBlank(message = "故事名称不能为空")
    private String name;

    @NotBlank(message = "核心创意不能为空")
    private String coreIdea;

    private String initialStageDirection;

    // TASK-120: story default target chapter length (nullable; DB default 3000).
    private Integer defaultTargetCharacters;

    // v0.1.1 Phase 6 (TASK-150): long-form target chapter count (nullable).
    private Integer targetChapterCount;

    // TASK-113: free-text writing style hint (Phase 8 will surface it to the Writer).
    private String writingStyle;

    @Valid
    private List<ConstraintInput> constraints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoreIdea() {
        return coreIdea;
    }

    public void setCoreIdea(String coreIdea) {
        this.coreIdea = coreIdea;
    }

    public String getInitialStageDirection() {
        return initialStageDirection;
    }

    public void setInitialStageDirection(String initialStageDirection) {
        this.initialStageDirection = initialStageDirection;
    }

    public Integer getDefaultTargetCharacters() {
        return defaultTargetCharacters;
    }

    public void setDefaultTargetCharacters(Integer defaultTargetCharacters) {
        this.defaultTargetCharacters = defaultTargetCharacters;
    }

    public Integer getTargetChapterCount() {
        return targetChapterCount;
    }

    public void setTargetChapterCount(Integer targetChapterCount) {
        this.targetChapterCount = targetChapterCount;
    }

    public String getWritingStyle() {
        return writingStyle;
    }

    public void setWritingStyle(String writingStyle) {
        this.writingStyle = writingStyle;
    }

    public List<ConstraintInput> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ConstraintInput> constraints) {
        this.constraints = constraints;
    }
}
