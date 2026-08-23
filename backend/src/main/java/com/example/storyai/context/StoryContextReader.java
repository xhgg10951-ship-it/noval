package com.example.storyai.context;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.arc.model.Arc;
import com.example.storyai.arc.service.ArcService;
import com.example.storyai.chapter.model.Chapter;
import com.example.storyai.chapter.service.ChapterService;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.MemoryTypes;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.model.StoryMemory;
import com.example.storyai.memory.service.MemoryService;
import com.example.storyai.stage.model.ChapterPlan;
import com.example.storyai.stage.model.Stage;
import com.example.storyai.stage.service.StageService;
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
    private final StageService stageService;
    // v0.1.1 Phase 6 (TASK-154): long-form position needs the current arc.
    private final ArcService arcService;

    public StoryContextReader(StoryService storyService,
                              MemoryService memoryService,
                              ChapterService chapterService,
                              StageService stageService,
                              ArcService arcService) {
        this.storyService = storyService;
        this.memoryService = memoryService;
        this.chapterService = chapterService;
        this.stageService = stageService;
        this.arcService = arcService;
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
                .filter(m -> m != null && m.isActive())
                .filter(m -> !MemoryTypes.TRANSIENT_DETAIL.equals(m.getType()))
                .filter(m -> m.getImportance() > 2)
                .sorted(Comparator
                        .comparingInt(StoryMemory::getImportance).reversed()
                        .thenComparing(m -> m.getId() == null ? Long.MAX_VALUE : m.getId()))
                .limit(PLANNER_MEMORY_CAP)
                .map(m -> new PlanStageRequest.MemoryItem(m.getType(), m.getSubject(), m.getDescription()))
                .toList();
    }

    /** RH-05 / HH-002: Planner context stays useful and predictably bounded. */
    private static final int PLANNER_MEMORY_CAP = 30;

    /**
     * Story memories mapped to the Writer request shape (see
     * {@link #getWriterStateItems} for why a separate mapper is needed).
     *
     * <p>v0.1.1 Phase 7 (TASK-164) — SELECTIVE memory, never the full dump:</p>
     * <ul>
     *   <li>always excluded: inactive rows, TRANSIENT_DETAIL, importance &lt;= 2
     *       (the "bread loop" class — transient details must not become anchors);</li>
     *   <li>always kept: PLOT_THREAD / FORESHADOWING (active threads and setups)
     *       and importance == 5 core facts;</li>
     *   <li>everything else kept up to a bounded cap, highest importance first,
     *       so long books cannot blow up the writer context.</li>
     * </ul>
     * No RAG / embeddings: structured selection only.
     */
    public List<GenerateChapterRequest.MemoryItem> getWriterMemoryItems(Long storyId) {
        return getWriterMemoryItems(storyId, null, null);
    }

    /**
     * RH-05 / HH-001 — deterministic structured relevance selection.
     * Uses only Arc, Stage, ChapterSpec and the existing relational memory
     * metadata; no embeddings or RAG are involved.
     */
    public List<GenerateChapterRequest.MemoryItem> getWriterMemoryItems(
            Long storyId, Stage stage, ChapterPlan plan) {
        List<StoryMemory> memories = memoryService.getStoryMemories(storyId);
        if (memories == null) return List.of();

        int nextChapterNumber = chapterService.nextChapterNumber(storyId);
        Arc arc = arcService.findCurrent(storyId, nextChapterNumber);
        String selectionContext = writerSelectionContext(arc, stage, plan);
        List<Chapter> chapters = chapterService.listByStory(storyId);
        Map<Long, Chapter> sourceChapters = chapters == null ? Map.of() : chapters.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Chapter::getId, c -> c));
        int currentChapterNumber = Math.max(0, nextChapterNumber - 1);

        List<StoryMemory> selected = memories.stream()
                .filter(m -> m != null && m.isActive())
                .filter(m -> !MemoryTypes.TRANSIENT_DETAIL.equals(m.getType()))
                .filter(m -> m.getImportance() > 2)
                .sorted((left, right) -> compareWriterMemories(
                        left, right, selectionContext, stage, arc,
                        sourceChapters, currentChapterNumber))
                .limit(WRITER_MEMORY_CAP)
                .toList();
        return selected.stream()
                .map(m -> new GenerateChapterRequest.MemoryItem(m.getType(), m.getSubject(), m.getDescription()))
                .toList();
    }

    /** TASK-164: hard bound on how many memories reach the Writer. */
    private static final int WRITER_MEMORY_CAP = 20;

    private int compareWriterMemories(StoryMemory left,
                                      StoryMemory right,
                                      String selectionContext,
                                      Stage stage,
                                      Arc arc,
                                      Map<Long, Chapter> sourceChapters,
                                      int currentChapterNumber) {
        int compared = Boolean.compare(right.getImportance() == 5, left.getImportance() == 5);
        if (compared != 0) return compared;
        compared = Boolean.compare(isActiveNarrativeThread(right), isActiveNarrativeThread(left));
        if (compared != 0) return compared;
        compared = Integer.compare(
                relevanceScore(right, selectionContext),
                relevanceScore(left, selectionContext));
        if (compared != 0) return compared;
        compared = Integer.compare(
                currentScopeScore(right, stage, arc, sourceChapters, currentChapterNumber),
                currentScopeScore(left, stage, arc, sourceChapters, currentChapterNumber));
        if (compared != 0) return compared;
        compared = Integer.compare(right.getImportance(), left.getImportance());
        if (compared != 0) return compared;
        long leftId = left.getId() == null ? Long.MAX_VALUE : left.getId();
        long rightId = right.getId() == null ? Long.MAX_VALUE : right.getId();
        return Long.compare(leftId, rightId);
    }

    private boolean isActiveNarrativeThread(StoryMemory memory) {
        return MemoryTypes.PLOT_THREAD.equals(memory.getType())
                || MemoryTypes.FORESHADOWING.equals(memory.getType());
    }

    private int relevanceScore(StoryMemory memory, String selectionContext) {
        if (selectionContext.isEmpty()) return 0;
        int score = 0;
        String subject = normalizeSelectionText(memory.getSubject());
        if (subject.length() >= 2 && selectionContext.contains(subject)) {
            score += 4;
        }
        String description = memory.getDescription();
        if (description != null) {
            for (String phrase : description.split("[\\s，。；、,:：！？]+")) {
                String normalized = normalizeSelectionText(phrase);
                if (normalized.length() >= 2 && selectionContext.contains(normalized)) {
                    score += 1;
                    break;
                }
            }
        }
        return score;
    }

    private int currentScopeScore(StoryMemory memory,
                                  Stage stage,
                                  Arc arc,
                                  Map<Long, Chapter> sourceChapters,
                                  int currentChapterNumber) {
        if ("STORY".equalsIgnoreCase(memory.getScope())) return 1;
        Chapter source = memory.getSourceChapterId() == null
                ? null : sourceChapters.get(memory.getSourceChapterId());
        if (source == null) return 0;
        if ("CHAPTER".equalsIgnoreCase(memory.getScope())
                && source.getChapterNumber() == currentChapterNumber) {
            return 4;
        }
        if ("STAGE".equalsIgnoreCase(memory.getScope())
                && stage != null && Objects.equals(source.getStageId(), stage.getId())) {
            return 3;
        }
        if ("ARC".equalsIgnoreCase(memory.getScope()) && arc != null
                && source.getChapterNumber() >= arc.getTargetStartChapter()
                && source.getChapterNumber() <= arc.getTargetEndChapter()) {
            return 2;
        }
        return 0;
    }

    private String writerSelectionContext(Arc arc, Stage stage, ChapterPlan plan) {
        StringBuilder context = new StringBuilder();
        if (arc != null) {
            appendSelectionText(context, arc.getTitle());
            appendSelectionText(context, arc.getGoal());
        }
        if (stage != null) appendSelectionText(context, stage.getDirection());
        if (plan != null) {
            appendSelectionText(context, plan.getGoal());
            appendSelectionText(context, plan.getExpectedProgress());
            appendSelectionText(context, plan.getMustAdvance());
            appendSelectionText(context, plan.getMustNotDo());
            appendSelectionText(context, plan.getStoryBeats());
            appendSelectionText(context, plan.getEndingIntent());
        }
        return normalizeSelectionText(context.toString());
    }

    private void appendSelectionText(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) target.append(' ').append(value);
    }

    private String normalizeSelectionText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。；、：！？]+", "");
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

    // ---- TASK-107: Planner continuation context v2 ----

    /**
     * Relationships mapped to the Planner request shape. {@link PlanStageRequest}
     * and {@link GenerateChapterRequest} use distinct nested {@code RelationshipItem}
     * types (same shape, different enclosing class), so the Planner needs its own
     * mapper.
     */
    public List<PlanStageRequest.RelationshipItem> getPlannerRelationshipItems(Long storyId) {
        List<RelationshipState> rels = memoryService.getRelationships(storyId);
        if (rels == null) return List.of();
        return rels.stream()
                .map(r -> new PlanStageRequest.RelationshipItem(r.getSubjectA(), r.getSubjectB(), r.getDescription()))
                .toList();
    }

    /**
     * Current (latest) chapter number of the story, or {@code null} when the
     * story has no chapters yet. Derived from {@code ChapterService.nextChapterNumber}
     * which returns {@code max + 1} (minimum 1), so the latest is {@code max}.
     */
    public Integer getCurrentChapterNumber(Long storyId) {
        int next = chapterService.nextChapterNumber(storyId); // max + 1 (min 1)
        int current = next - 1;
        return current < 1 ? null : current;
    }

    /**
     * Recent chapter summaries as a list (most recent last), capped at
     * {@code maxChapters}. Empty list when the story has no chapters.
     */
    public List<String> getRecentChapterSummaries(Long storyId, int maxChapters) {
        List<Chapter> chapters = chapterService.listByStory(storyId);
        if (chapters == null || chapters.isEmpty()) return List.of();
        return chapters.stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber))
                .skip(Math.max(0, chapters.size() - maxChapters))
                .map(Chapter::getSummary)
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }

    /**
     * One-line summaries of already-completed stages ("第 N 阶段：<direction>"),
     * ordered by stage creation. Completed stages are those with status
     * {@code COMPLETED}; this is a lightweight stage-level continuity signal
     * (no dedicated stage-summary column exists in v0.1 — see frozen plan).
     */
    public List<String> getCompletedStageSummaries(Long storyId) {
        List<Stage> stages = stageService.listStages(storyId);
        if (stages == null || stages.isEmpty()) return List.of();
        return stages.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .sorted(Comparator.comparing(
                        s -> s.getCreatedAt() == null ? java.time.LocalDateTime.MAX : s.getCreatedAt()))
                .map(s -> "第 " + s.getId() + " 阶段：" + (s.getDirection() == null ? "" : s.getDirection()))
                .toList();
    }

    // ---- TASK-108: Continuation Anchor Assembly ----

    /**
     * Builds the structured continuation anchor used by the Planner to avoid
     * restarting the story. Carries the latest chapter number, current location,
     * active characters, the immediate goal, the last chapter summary and a tail
     * excerpt of the last chapter's content.
     *
     * <p>{@code location} / {@code activeCharacters} / {@code immediateGoal} are
     * derived from the most recent structured CurrentState + StoryMemory rows
     * (best-effort, non-LLM); they are left null/empty when no such signal
     * exists. {@code lastChapterSummary} / {@code lastChapterEnding} are taken
     * directly from the latest chapter.</p>
     */
    public PlanStageRequest.ContinuationAnchor buildContinuationAnchor(Long storyId,
                                                                       int endingExcerptChars) {
        List<Chapter> chapters = chapterService.listByStory(storyId);
        Integer lastChapterNumber = getCurrentChapterNumber(storyId);
        String lastSummary = null;
        String lastEnding = null;
        if (chapters != null && !chapters.isEmpty()) {
            Chapter last = chapters.stream()
                    .max(Comparator.comparing(Chapter::getChapterNumber))
                    .orElse(null);
            if (last != null) {
                lastSummary = last.getSummary();
                String content = last.getContent();
                if (content != null && !content.isEmpty() && endingExcerptChars > 0) {
                    lastEnding = content.length() <= endingExcerptChars
                            ? content
                            : content.substring(content.length() - endingExcerptChars);
                }
            }
        }

        // Derive location / characters / immediate goal from structured memory.
        String location = null;
        List<String> activeCharacters = List.of();
        String immediateGoal = null;
        List<CurrentState> states = memoryService.getCurrentState(storyId);
        if (states != null) {
            for (CurrentState s : states) {
                if (s == null || s.getCategory() == null) continue;
                if (location == null && "LOCATION".equalsIgnoreCase(s.getCategory())) {
                    location = s.getValue();
                }
                if (immediateGoal == null && "CURRENT_GOAL".equalsIgnoreCase(s.getCategory())) {
                    immediateGoal = s.getValue();
                }
            }
        }
        List<RelationshipState> relationships = memoryService.getRelationships(storyId);
        if (relationships != null) {
            Set<String> characters = new LinkedHashSet<>();
            for (RelationshipState relationship : relationships) {
                if (relationship == null) continue;
                addReliableCharacter(characters, relationship.getSubjectA());
                addReliableCharacter(characters, relationship.getSubjectB());
            }
            activeCharacters = List.copyOf(characters);
        }

        return new PlanStageRequest.ContinuationAnchor(
                lastChapterNumber,
                location,
                activeCharacters,
                immediateGoal,
                lastSummary,
                lastEnding
        );
    }

    private void addReliableCharacter(Set<String> characters, String subject) {
        if (subject != null && !subject.isBlank() && !"(story)".equalsIgnoreCase(subject)) {
            characters.add(subject);
        }
    }

    /**
     * TASK-154 — long-form position for the Planner: the story's total target
     * chapter count, the current chapter number, and the arc whose range covers
     * that position (falling back to the ACTIVE arc). All fields best-effort:
     * a short story without arcs yields nulls and the block is omitted.
     */
    public PlanStageRequest.LongFormPosition buildLongFormPosition(Long storyId) {
        var story = storyService.getStory(storyId);
        Integer current = getCurrentChapterNumber(storyId);
        int nextChapterNumber = current == null ? 1 : current + 1;
        com.example.storyai.arc.model.Arc arc =
                arcService.findCurrent(storyId, nextChapterNumber);
        if (story.getTargetChapterCount() == null && current == null && arc == null) {
            return null; // nothing long-form about this project — omit the block
        }
        return new PlanStageRequest.LongFormPosition(
                story.getTargetChapterCount(),
                current,
                arc == null ? null : arc.getTitle(),
                arc == null ? null : arc.getGoal(),
                arc == null ? null : arc.getTargetStartChapter(),
                arc == null ? null : arc.getTargetEndChapter()
        );
    }

    /** Frozen Writer context: long-form target and the chapter being written. */
    public GenerateChapterRequest.LongFormPosition buildWriterLongFormPosition(
            Long storyId, Integer chapterNumber) {
        var story = storyService.getStory(storyId);
        if (story.getTargetChapterCount() == null && chapterNumber == null) {
            return null;
        }
        return new GenerateChapterRequest.LongFormPosition(
                story.getTargetChapterCount(), chapterNumber);
    }

    /** Frozen Writer context: the Arc covering the chapter being written. */
    public GenerateChapterRequest.CurrentArc buildWriterCurrentArc(
            Long storyId, Integer chapterNumber) {
        int effectiveChapter = chapterNumber == null ? chapterService.nextChapterNumber(storyId)
                : chapterNumber;
        Arc arc = arcService.findCurrent(storyId, effectiveChapter);
        if (arc == null) {
            return null;
        }
        return new GenerateChapterRequest.CurrentArc(
                arc.getTitle(),
                arc.getGoal(),
                arc.getTargetStartChapter(),
                arc.getTargetEndChapter());
    }
}
