package com.example.storyai.common.exception;

/**
 * Thrown when a stage has no remaining ungenerated chapter plans. Mapped to
 * HTTP 409 by {@code GlobalExceptionHandler} so the UI can show a friendly
 * "all chapters generated" state instead of a crash.
 */
public class NoPendingChapterException extends RuntimeException {

    public NoPendingChapterException(Long stageId) {
        super("阶段 " + stageId + " 的所有章节计划均已生成，没有可继续生成的下一章");
    }
}
