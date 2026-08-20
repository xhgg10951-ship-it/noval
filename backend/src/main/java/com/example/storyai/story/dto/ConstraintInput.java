package com.example.storyai.story.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A single story constraint input (type + content).
 */
public class ConstraintInput {

    @NotBlank(message = "约束类型不能为空")
    private String type;

    @NotBlank(message = "约束内容不能为空")
    private String content;

    private Integer sortOrder;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
