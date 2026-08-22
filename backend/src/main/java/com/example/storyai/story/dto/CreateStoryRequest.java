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

    public List<ConstraintInput> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ConstraintInput> constraints) {
        this.constraints = constraints;
    }
}
