package com.example.storyai.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.ExtractMemoryRequest;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.chapter.service.GenerationOrchestrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TASK-132 — Generation Reliability Regression Suite (Phase 3 Gate).
 *
 * <pre>
 * extraction failure after save -> chapter retained, retry re-extracts SAME chapter
 * async progress                -> POST returns immediately, polling observes phases
 * pause                         -> stops at the next checkpoint, chapters retained
 * stop                          -> terminates, chapters retained
 * stage completion              -> Job COMPLETED => Stage COMPLETED (AC-113)
 * duplicate prevention          -> completed stage cannot regenerate chapters
 * </pre>
 *
 * <p>Writer/extraction failures BEFORE save are covered by
 * {@link GenerationModesIntegrationTest} (AT-M01/M02). The Python AI Service is
 * mocked; orchestration + persistence are real.</p>
 *
 * <p>TASK-127 runs generation on a background executor, so these tests are NOT
 * {@code @Transactional}: the worker needs committed rows. Cleanup deletes the
 * created story; all child tables cascade from story (V1..V11 FKs).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class GenerationReliabilityRegressionTest {

    private static final long POLL_TIMEOUT_MS = 60_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GenerationOrchestrationService orchestrationService;

    private final List<Long> createdStoryIds = new ArrayList<>();

    @AfterEach
    void cleanupCreatedStories() {
        for (Long storyId : createdStoryIds) {
            jdbcTemplate.update("DELETE FROM story WHERE id = ?", storyId);
        }
        createdStoryIds.clear();
    }

    // ---- helpers ----

    private Long createStory() throws Exception {
        String body = """
                {"name":"可靠性回归故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
                """;
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
        createdStoryIds.add(id);
        return id;
    }

    private PlanStageResponse planOf(int count) {
        return new PlanStageResponse(count, IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                        i, "目标：第" + i + "章", "推进 " + i + "/" + count,
                        null, null, null, null, null))
                .toList());
    }

    private void stubWriter() {
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章",
                            "本章围绕「" + req.chapterGoal() + "」展开。",
                            "推进：" + req.chapterGoal());
                });
    }

    /** Writer stub that blocks in the FIRST call until the gate opens (long LLM call). */
    private CountDownLatch stubWriterGatedOnFirstCall() {
        CountDownLatch gate = new CountDownLatch(1);
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    if (req.chapterOrder() == 1) {
                        gate.await(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章",
                            "本章围绕「" + req.chapterGoal() + "」展开。",
                            "推进：" + req.chapterGoal());
                });
        return gate;
    }

    private long createActiveStage(Long storyId, int planCount) throws Exception {
        long stageId = createPlanningStage(storyId, planCount);
        mockMvc.perform(post("/api/stages/{id}/confirm", stageId)).andExpect(status().isOk());
        return stageId;
    }

    private long createPlanningStage(Long storyId, int planCount) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class))).thenReturn(planOf(planCount));
        stubWriter();
        stubExtractor();
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        return stageId;
    }

    private void stubExtractor() {
        when(aiServiceClient.extractMemory(any(ExtractMemoryRequest.class))).thenReturn(
                new com.example.storyai.ai.dto.ExtractMemoryResponse(List.of()));
    }

    private long startContinuous(long stageId) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "CONTINUOUS"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private JsonNode getJob(long jobId) throws Exception {
        String body = mockMvc.perform(get("/api/generation-jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    private JsonNode awaitTerminal(long jobId) throws Exception {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonNode node = getJob(jobId);
            String status = node.get("status").asText();
            if (List.of("COMPLETED", "FAILED", "STOPPED", "PAUSED").contains(status)) {
                return node;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("job " + jobId + " did not reach a terminal state in time");
    }

    private void awaitPhase(long jobId, String phase, int maxIndex) throws Exception {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonNode node = getJob(jobId);
            if (phase.equals(node.get("phase").asText())
                    && node.get("currentPlanIndex").asInt() <= maxIndex) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("job " + jobId + " never entered phase " + phase);
    }

    private static String normErr(JsonNode node) {
        return node.get("lastError").isNull() ? null : node.get("lastError").asText();
    }

    /**
     * After retry/continue the job row may still show the PREVIOUS terminal state
     * (the worker flips it asynchronously). Waits for a terminal state that is
     * DIFFERENT from the given pre-restart snapshot — by status, progress index,
     * or lastError — so a stale read can never satisfy the wait.
     */
    private JsonNode awaitChangedTerminal(long jobId, JsonNode snapshot) throws Exception {
        String prevStatus = snapshot.get("status").asText();
        int prevIndex = snapshot.get("currentPlanIndex").asInt();
        String prevError = normErr(snapshot);
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonNode node = getJob(jobId);
            String status = node.get("status").asText();
            if (List.of("COMPLETED", "FAILED", "STOPPED", "PAUSED").contains(status)) {
                int index = node.get("currentPlanIndex").asInt();
                if (!status.equals(prevStatus) || index != prevIndex
                        || !java.util.Objects.equals(normErr(node), prevError)) {
                    return node;
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("job " + jobId + " never left its previous terminal state "
                + prevStatus + "/" + prevIndex);
    }

    private JsonNode jobSnapshot(long jobId) throws Exception {
        return getJob(jobId);
    }

    private List<int[]> listChapters(long stageId) throws Exception {
        String body = mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<int[]> result = new ArrayList<>();
        for (JsonNode c : objectMapper.readTree(body)) {
            result.add(new int[]{c.get("id").asInt(), c.get("chapterNumber").asInt()});
        }
        return result;
    }

    // ---- extraction failure AFTER save: chapter retained; retry re-extracts SAME chapter ----

    @Test
    void extractionFailureAfterSaveRetainsChapterAndRetryReextractsSameChapter() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 3);

        // first extraction call fails (after the chapter row was persisted), later ones succeed
        when(aiServiceClient.extractMemory(any(ExtractMemoryRequest.class)))
                .thenThrow(new RuntimeException("记忆抽取服务暂时不可用"))
                .thenAnswer(inv -> new com.example.storyai.ai.dto.ExtractMemoryResponse(List.of()));

        long jobId = startContinuous(stageId);

        JsonNode failed = awaitTerminal(jobId);
        assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        assertThat(failed.get("lastError").asText()).contains("记忆抽取服务暂时不可用");

        // the chapter that already persisted is RETAINED (not rolled back, not duplicated)
        List<int[]> afterFailure = listChapters(stageId);
        assertThat(afterFailure).hasSize(1);

        mockMvc.perform(post("/api/generation-jobs/{id}/retry", jobId))
                .andExpect(status().isOk());
        JsonNode recovered = awaitChangedTerminal(jobId, failed);
        assertThat(recovered.get("status").asText()).isEqualTo("COMPLETED");

        // exactly 3 chapters, numbers 1..3, no duplicates and no skipped plan
        List<int[]> chapters = listChapters(stageId);
        assertThat(chapters).hasSize(3);
        assertThat(chapters.stream().map(c -> c[1]).sorted().toList())
                .containsExactly(1, 2, 3);

        // proof of SAME-chapter retry: 4 extraction calls total —
        // #1 order=1 (failed), #2 order=1 (re-extract of the SAME chapter), #3/#4 orders 2,3
        ArgumentCaptor<ExtractMemoryRequest> captor = ArgumentCaptor.forClass(ExtractMemoryRequest.class);
        verify(aiServiceClient, times(4)).extractMemory(captor.capture());
        List<ExtractMemoryRequest> calls = captor.getAllValues();
        assertThat(calls.get(0).chapterOrder()).isEqualTo(1);
        assertThat(calls.get(1).chapterOrder()).isEqualTo(1);
        assertThat(calls.get(1).chapterContent()).isEqualTo(calls.get(0).chapterContent());
        assertThat(calls.get(2).chapterOrder()).isEqualTo(2);
        assertThat(calls.get(3).chapterOrder()).isEqualTo(3);

        // the writer produced content only 3 times (chapter 1 content was NOT regenerated)
        verify(aiServiceClient, times(3)).generateChapter(any(GenerateChapterRequest.class));
    }

    // ---- TASK-129: pause stops at the next checkpoint, chapters retained ----

    @Test
    void pauseStopsAtNextCheckpointAndRetainsChapters() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 3);

        CountDownLatch gate = stubWriterGatedOnFirstCall();
        long jobId = startContinuous(stageId);

        // wait until the worker is inside the first writer call
        awaitPhase(jobId, "WRITING", 0);

        mockMvc.perform(post("/api/generation-jobs/{id}/pause", jobId))
                .andExpect(status().isOk());

        gate.countDown(); // release the writer; chapter 1 finishes safely

        JsonNode paused = awaitTerminal(jobId);
        assertThat(paused.get("status").asText()).isEqualTo("PAUSED");
        assertThat(listChapters(stageId)).hasSize(1);

        // resume: remaining plans run to completion
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode done = awaitChangedTerminal(jobId, paused);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(listChapters(stageId)).hasSize(3);
    }

    // ---- TASK-130: stop terminates the run, completed chapters retained ----

    @Test
    void stopTerminatesRunAndRetainsCompletedChapters() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 3);

        CountDownLatch gate = stubWriterGatedOnFirstCall();
        long jobId = startContinuous(stageId);
        awaitPhase(jobId, "WRITING", 0);

        mockMvc.perform(post("/api/generation-jobs/{id}/stop", jobId))
                .andExpect(status().isOk());

        gate.countDown();

        JsonNode stopped = awaitTerminal(jobId);
        assertThat(stopped.get("status").asText()).isEqualTo("STOPPED");
        assertThat(listChapters(stageId)).hasSize(1);

        // STOPPED keeps the finished chapter; no further generation happens
        assertThat(getJob(jobId).get("currentPlanIndex").asInt()).isEqualTo(1);
    }

    // ---- TASK-131 / AC-113: job completion transitions the Stage to COMPLETED ----

    @Test
    void jobCompletionTransitionsStageToCompleted() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);

        long jobId = startContinuous(stageId);
        JsonNode done = awaitTerminal(jobId);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");

        String stageBody = mockMvc.perform(get("/api/stages/{id}", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(stageBody).get("status").asText()).isEqualTo("COMPLETED");
    }

    // ---- duplicate chapter prevention: a fully generated stage cannot regenerate ----

    @Test
    void completedStageCannotGenerateDuplicateChapters() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);

        long firstJob = startContinuous(stageId);
        assertThat(awaitTerminal(firstJob).get("status").asText()).isEqualTo("COMPLETED");
        List<int[]> before = listChapters(stageId);
        assertThat(before).hasSize(2);

        // starting again finds NO active remaining plan: the run is a no-op that
        // completes without touching the existing chapters (plans are COMPLETED
        // + inactive after their chapters were generated — TASK-134/135).
        long secondJob = startContinuous(stageId);
        JsonNode rerun = awaitTerminal(secondJob);
        assertThat(rerun.get("status").asText()).isEqualTo("COMPLETED");

        List<int[]> after = listChapters(stageId);
        assertThat(after).hasSize(2);
        assertThat(after.stream().map(c -> c[1]).sorted().toList())
                .containsExactly(1, 2);
        // same chapter rows, nothing regenerated
        assertThat(after.stream().map(c -> c[0]).sorted().toList())
                .containsExactlyElementsOf(before.stream().map(c -> c[0]).sorted().toList());
    }

    // ---- RH-06 / HH-005 / AC-H08: one active job per Stage ----

    @Test
    void startRejectsPendingRunningAndPausedJobsForSameStage() throws Exception {
        SoftAssertions softly = new SoftAssertions();
        for (String existingStatus : List.of("PENDING", "RUNNING", "PAUSED")) {
            Long storyId = createStory();
            long stageId = createActiveStage(storyId, 1);
            jdbcTemplate.update("""
                    INSERT INTO generation_job
                        (stage_id, mode, current_plan_index, total, status, phase,
                         pause_requested, stop_requested)
                    VALUES (?, 'CONTINUOUS', 0, 1, ?, 'PLANNING', 0, 0)
                    """, stageId, existingStatus);

            MvcResult duplicate = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                            .param("mode", "CONTINUOUS"))
                    .andReturn();
            int actualStatus = duplicate.getResponse().getStatus();
            if (actualStatus == 200) {
                long wronglyCreatedJob = objectMapper.readTree(
                        duplicate.getResponse().getContentAsString()).get("id").asLong();
                awaitTerminal(wronglyCreatedJob);
            }

            softly.assertThat(actualStatus)
                    .as("duplicate start while existing job is %s", existingStatus)
                    .isEqualTo(409);
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM generation_job WHERE stage_id = ?",
                    Integer.class, stageId);
            softly.assertThat(count)
                    .as("job row count while existing job is %s", existingStatus)
                    .isEqualTo(1);
        }
        softly.assertAll();
    }

    // ---- RH-06 / HH-006: bounded executor is owned by Spring ----

    @Test
    void generationUsesSpringManagedBoundedExecutor() {
        ThreadPoolTaskExecutor executor = applicationContext.getBean(
                "generationTaskExecutor", ThreadPoolTaskExecutor.class);

        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity())
                .isBetween(1, 1000);
        assertThat(ReflectionTestUtils.getField(orchestrationService, "executor"))
                .isSameAs(executor);
    }

    // ---- Reopened RH-06: lifecycle commands must not create illegal workers ----

    @Test
    void generationCannotStartBeforeStagePlanIsConfirmed() throws Exception {
        Long storyId = createStory();
        long stageId = createPlanningStage(storyId, 1);

        MvcResult result = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "CONTINUOUS"))
                .andReturn();
        if (result.getResponse().getStatus() == 200) {
            long wronglyStarted = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();
            awaitTerminal(wronglyStarted);
        }

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(listChapters(stageId)).isEmpty();
    }

    @Test
    void stoppedJobCannotBeContinued() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 1);
        jdbcTemplate.update("""
                INSERT INTO generation_job
                    (stage_id, mode, current_plan_index, total, status, phase,
                     pause_requested, stop_requested)
                VALUES (?, 'CONTINUOUS', 0, 1, 'STOPPED', 'CHECKPOINT', 0, 1)
                """, stageId);
        long jobId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM generation_job WHERE stage_id = ?", Long.class, stageId);

        MvcResult result = mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andReturn();
        if (result.getResponse().getStatus() == 200) {
            awaitTerminal(jobId);
        }

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(listChapters(stageId)).isEmpty();
    }

    @Test
    void runningJobCannotBeRetriedOrDispatchAnotherWorker() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 1);
        jdbcTemplate.update("""
                INSERT INTO generation_job
                    (stage_id, mode, current_plan_index, total, status, phase,
                     pause_requested, stop_requested)
                VALUES (?, 'CONTINUOUS', 0, 1, 'RUNNING', 'WRITING', 0, 0)
                """, stageId);
        long jobId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM generation_job WHERE stage_id = ?", Long.class, stageId);

        mockMvc.perform(post("/api/generation-jobs/{id}/retry", jobId))
                .andExpect(status().isConflict());
        assertThat(listChapters(stageId)).isEmpty();
    }

    @Test
    void stopRequestedDuringStepModeWinsAtTheSafeCheckpoint() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);
        CountDownLatch gate = stubWriterGatedOnFirstCall();

        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "STEP"))
                .andExpect(status().isOk())
                .andReturn();
        long jobId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();
        awaitPhase(jobId, "WRITING", 0);

        mockMvc.perform(post("/api/generation-jobs/{id}/stop", jobId))
                .andExpect(status().isOk());
        gate.countDown();

        JsonNode stopped = awaitTerminal(jobId);
        assertThat(stopped.get("status").asText()).isEqualTo("STOPPED");
        assertThat(listChapters(stageId)).hasSize(1);
    }

    @Test
    void stoppingAnAlreadyPausedStepJobConvergesImmediatelyToStopped() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);

        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "STEP"))
                .andExpect(status().isOk())
                .andReturn();
        long jobId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();
        JsonNode paused = awaitTerminal(jobId);
        assertThat(paused.get("status").asText()).isEqualTo("PAUSED");
        assertThat(listChapters(stageId)).hasSize(1);

        mockMvc.perform(post("/api/generation-jobs/{id}/stop", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.phase").value("CHECKPOINT"));

        assertThat(getJob(jobId).get("status").asText()).isEqualTo("STOPPED");
        assertThat(listChapters(stageId)).hasSize(1);
    }

    @Test
    void completionReconcilesAnyEarlierUnstableChapterMemory() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);
        CountDownLatch secondWriterEntered = new CountDownLatch(1);
        CountDownLatch releaseSecondWriter = new CountDownLatch(1);
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    if (req.chapterOrder() == 2) {
                        secondWriterEntered.countDown();
                        releaseSecondWriter.await(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章", "正文", "摘要");
                });

        long jobId = startContinuous(stageId);
        assertThat(secondWriterEntered.await(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue();
        jdbcTemplate.update("""
                UPDATE chapter
                SET memory_extraction_status = 'STALE'
                WHERE stage_id = ? AND chapter_number = 1
                """, stageId);
        releaseSecondWriter.countDown();

        JsonNode done = awaitTerminal(jobId);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT memory_extraction_status FROM chapter
                WHERE stage_id = ? AND chapter_number = 1
                """, String.class, stageId)).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM stage WHERE id = ?", String.class, stageId))
                .isEqualTo("COMPLETED");
    }
}
