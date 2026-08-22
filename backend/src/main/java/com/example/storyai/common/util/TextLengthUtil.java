package com.example.storyai.common.util;

/**
 * Simple, deterministic character counting for chapter length control (TASK-119).
 *
 * <p>Counting rule (kept intentionally simple and consistent): every Unicode
 * code point counts as one character. This treats a CJK ideograph and a Latin
 * letter identically (one each), which matches how authors informally count
 * "字数" for Chinese prose and avoids fragile width/normalization heuristics.
 * Surrogate pairs (rare in our content) are counted as a single code point.</p>
 */
public final class TextLengthUtil {

    private TextLengthUtil() {
    }

    /** Counts characters by code point (CJK and Latin both count as 1). */
    public static int countCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.codePointCount(0, text.length());
    }
}
