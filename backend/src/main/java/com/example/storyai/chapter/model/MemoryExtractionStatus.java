package com.example.storyai.chapter.model;

/**
 * Lifecycle status of memory extraction for a single generated chapter.
 *
 * <p>TASK-123: makes the extraction state machine explicit so recovery logic
 * (TASK-125/126) can decide whether to (re)extract or advance.</p>
 */
public final class MemoryExtractionStatus {

    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String STALE = "STALE";

    private MemoryExtractionStatus() {
    }

    public static boolean isValid(String status) {
        return PENDING.equals(status) || COMPLETED.equals(status)
                || FAILED.equals(status) || STALE.equals(status);
    }
}
