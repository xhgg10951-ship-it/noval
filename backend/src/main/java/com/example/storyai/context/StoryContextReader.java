package com.example.storyai.context;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.chapter.service.ChapterService;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;
import com.example.storyai.memory.service.MemoryService;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * Reusable Story Context assembly (Phase 1 / TASK-105).
 *
 * <p>Loads the structured story context that the Planner and Writer need but
 * v0.1 never wired: constraints, current state, story memories, relationships,
 * and recent chapter continuity context. Centralises the DTO mapping so every
 * AI call builds from the same source (no duplicated stream-mapping across
 * services).</p>
 *
 * <p>Read-only: everything is loaded through existing services / mappers; this
 * component performs no writes. Complex Memory selection / ranking is explicitly
 * out of scope for TASK-105 (see frozen plan).</p>
 */
@Service
public class StoryContextReader {

    private final StoryService storyService;
    private final MemoryService memoryService;
    private final ChapterService chapterService;

    public StoryContextReader(StoryService storyService,
                              MemoryService memoryService,
                              ChapterService chapterService) {
        this.storyService = storyService;
        this.memoryService = memoryService;
        this.chapterService = chapterService;
    }

    // ---- constraints ----

    public List<PlanStageRequest.ConstraintItem> getConstraintItems(Long storyId) {
        List<StoryConstraint> constraints = storyService.getConstraints(storyId);
        if (constraints == null) return List.of();
        return constraints.stream()
                .map(c -> new PlanStageRequest.ConstraintItem(c.getType(), c.getContent()))
                .toList();
    }

    // ---- current state ----

    public List<PlanStageRequest.StateItem> getCurrentStateItems(Long storyId) {
        List<CurrentState> states = memoryService.getCurrentState(storyId);
        if (states == null) return List.of();
        return states.stream()
                .map(s -> new PlanStageRequest.StateItem(s.getCategory(), s.getSubject(), s.getField(), s.getValue()))
                .toList();
    }

    /**
     * Current state mapped to the Writer request shape. The nested
     * {@code StateItem} records of {@link PlanStageRequest} and
     * {@link GenerateChapterRequest} are distinct types (same field shape,
     * different enclosing class), so the Writer needs its own mapper.
     */
    public List<GenerateChapterRequest.StateItem> getWriterStateItems(Long storyId) {
        List<CurrentState> states = memoryService.getCurrentState(storyId);
        if (states == null) return List.of();
        return states.stream()
                .map(s -> new GenerateChapterRequest.StateItem(s.getCategory(), s.getSubject(), s.getField(), s.getValue()))
                .toList();
    }

    // ---- story memories ----

    public List<PlanStageRequest.MemoryItem> getStoryMemoryItems(Long storyId) {
        List<StoryMemory> memories = memoryService.getStoryMemories(storyId);
        if (memories == null) return List.of();
        return memories.stream()
                .map(m -> new PlanStageRequest.MemoryItem(m.getType(), m.getSubject(), m.getDescription()))
                .toList();
    }

    /**
     * Story memories mapped to the Writer request shape (see
     * {@link #getWriterStateItems} for why a separate mapper is needed).
     */
    public List<GenerateChapterRequest.MemoryItem> getWriterMemoryItems(Long storyId) {
        List<StoryMemory> memories = memoryService.getStoryMemories(storyId);
        if (memories == null) return List.of();
        return memories.stream()
                .map(m -> new GenerateChapterRequest.MemoryItem(m.getType(), m.getSubject(), m.getDescription()))
                .toList();
    }

    // ---- relationships ----

    public List<GenerateChapterRequest.RelationshipItem> getRelationshipItems(Long storyId) {
        List<RelationshipState> rels = memoryService.getRelationships(storyId);
        if (rels == null) return List.of();
        return rels.stream()
                .map(r -> new GenerateChapterRequest.RelationshipItem(r.getSubjectA(), r.getSubjectB(), r.getDescription()))
                .toList();
    }

    // ---- recent continuity context ----

    /**
     * Concatenates the summaries of the most recent {@code maxChapters} chapters
     * (by chapter number, ascending) into a single continuity string. Returns ""
     * when the story has no chapters yet.
     *
     * <p>Lightweight continuity anchor for Planner / Writer requests; TASK-111
     * upgrades the Writer path to multiple summaries + last-chapter ending
     * excerpt.</p>
     */
    public String getRecentContext(Long storyId, int maxChapters) {
        List<Chapter> chapters = chapterService.listByStory(storyId);
        if (chapters == null || chapters.isEmpty()) return "";
        return chapters.stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber))
                .skip(Math.max(0, chapters.size() - maxChapters))
                .map(Chapter::getSummary)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * TASK-111 — upgraded Writer continuity context: the summaries of the most
     * recent {@code maxChapters} chapters PLUS a tail excerpt of the latest
     * chapter's content (its ending), so a single summary can no longer be the
     * only continuity anchor.
     *
     * @param endingExcerptChars max characters taken from the end of the latest
     *                            chapter's content (0 = omit the excerpt)
     */
    public String getRecentContextWithEnding(Long storyId, int maxChapters, int endingExcerptChars) {
        List<Chapter> chapters = chapterService.listByStory(storyId);
        if (chapters == null || chapters.isEmpty()) return "";
        List<Chapter> sorted = chapters.stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber))
                .toList();
        List<Chapter> recent = sorted.stream()
                .skip(Math.max(0, sorted.size() - maxChapters))
                .toList();

        StringBuilder sb = new StringBuilder();
        for (Chapter c : recent) {
            if (c.getSummary() != null && !c.getSummary().isBlank()) {
                sb.append("【第 ").append(c.getChapterNumber()).append(" 章 摘要】\n")
                  .append(c.getSummary()).append("\n\n");
            }
        }
        if (endingExcerptChars > 0 && !recent.isEmpty()) {
            Chapter last = recent.get(recent.size() - 1);
            String content = last.getContent();
            if (content != null && !content.isEmpty()) {
                String excerpt = content.length() <= endingExcerptChars
                        ? content
                        : content.substring(content.length() - endingExcerptChars);
                sb.append("【上一章结尾】\n").append(excerpt).append("\n");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "" : result;
    }
}
