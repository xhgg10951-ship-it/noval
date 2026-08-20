package com.example.storyai.common.exception;

/**
 * Thrown when a domain resource (Story, Stage, ...) cannot be found by id.
 * Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
