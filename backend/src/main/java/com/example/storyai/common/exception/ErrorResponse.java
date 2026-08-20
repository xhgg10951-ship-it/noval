package com.example.storyai.common.exception;

import java.util.Map;

/**
 * Unified error body returned by the global exception handler (ARCHITECTURE §54).
 * Distinguishes Validation / Business / AI / Internal errors by {@code code}
 * instead of leaking a raw "500 Unknown Error".
 */
public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
