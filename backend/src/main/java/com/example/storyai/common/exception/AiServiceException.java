package com.example.storyai.common.exception;

/**
 * Thrown when the Python AI Service is unreachable or returns a response that
 * violates the structured contract. Mapped to HTTP 502 by the global handler
 * so callers can distinguish AI-boundary failures from internal bugs.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
