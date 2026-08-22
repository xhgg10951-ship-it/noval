package com.example.storyai.memory.model;

import java.util.Locale;
import java.util.Set;

/**
 * v0.1.1 Phase 7 (TASK-158) — the frozen Memory type contract.
 *
 * <p>Both ends (Python extractor output and Java processing) speak ONLY these
 * types. Legacy v0.1 free-text types are mapped onto them; anything unmappable
 * is NOT silently stored as a story fact — it routes to REVIEW instead.</p>
 */
public final class MemoryTypes {

    public static final String CURRENT_STATE = "CURRENT_STATE";
    public static final String RELATIONSHIP = "RELATIONSHIP";
    public static final String PLOT_FACT = "PLOT_FACT";
    public static final String PLOT_THREAD = "PLOT_THREAD";
    public static final String FORESHADOWING = "FORESHADOWING";
    public static final String WORLD_RULE = "WORLD_RULE";
    public static final String TRANSIENT_DETAIL = "TRANSIENT_DETAIL";

    public static final Set<String> ALL = Set.of(
            CURRENT_STATE, RELATIONSHIP, PLOT_FACT, PLOT_THREAD,
            FORESHADOWING, WORLD_RULE, TRANSIENT_DETAIL);

    /** Valid scope values (TASK-159). */
    public static final Set<String> SCOPES = Set.of("CHAPTER", "STAGE", "ARC", "STORY");

    private MemoryTypes() {
    }

    /**
     * Maps a raw extractor/story value onto the frozen enum. Returns null when
     * the value has no faithful mapping — callers must treat null as REVIEW,
     * never as a catch-all story fact.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "CURRENT_STATE":
                return CURRENT_STATE;
            case "RELATIONSHIP":
                return RELATIONSHIP;
            case "PLOT_FACT":
            case "EVENT":
            case "FACT":
            case "ANOMALY":
                return PLOT_FACT;
            case "PLOT_THREAD":
            case "THREAD":
            case "SECRET":
            case "PROMISE":
                return PLOT_THREAD;
            case "FORESHADOWING":
            case "FORESHADOW":
                return FORESHADOWING;
            case "WORLD_RULE":
            case "WORLD":
            case "RULE":
                return WORLD_RULE;
            case "TRANSIENT_DETAIL":
            case "DETAIL":
            case "NOISE":
                return TRANSIENT_DETAIL;
            default:
                return null; // unknown -> caller must route to REVIEW
        }
    }

    /** Clamps importance into 1..5; null/invalid falls back to 3. */
    public static int clampImportance(Integer raw) {
        if (raw == null) {
            return 3;
        }
        return Math.max(1, Math.min(5, raw));
    }

    /** Returns a valid scope or the STORY default for unknown/null values. */
    public static String normalizeScope(String raw) {
        if (raw != null && SCOPES.contains(raw.trim().toUpperCase(Locale.ROOT))) {
            return raw.trim().toUpperCase(Locale.ROOT);
        }
        return "STORY";
    }
}
