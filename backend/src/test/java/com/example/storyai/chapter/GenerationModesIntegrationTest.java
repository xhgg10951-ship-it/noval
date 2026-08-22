package com.example.storyai.chapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.common.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for multi-chapter generation modes (M5 / TASK-036..040).
 * Covers AT-L01 (Step-by-Step), AT-L02 (Continuous), AT-M01/M02 (failure + retry).
 * The Python AI Service is mocked; only the orchestration and persistence are real.
 *
 * <p>TASK-127: generation runs on a background executor, so these tests are NOT
 * {@code @Transactional} — the worker thread needs committed rows to see. Cleanup
 * deletes the created story; every child table cascades from story (V1..V5 FKs).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class GenerationModesIntegrationTest {

    private static final long POLL_TIMEOUT_MS = 60_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private final List<Long> createdStoryIds = new java.util.ArrayList<>();

    @AfterEach
    void cleanupCreatedStories() {
        for (Long storyId : createdStoryIds) {
            // all child tables (stage/chapter/chapter_plan/generation_job/memory_*) cascade
            jdbcTemplate.update("DELETE FROM story WHERE id = ?", storyId);
        }
        createdStoryIds.clear();
    }

    private Long createStory() throws Exception {
        String body = """
                {"name":"多章生成测试故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
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

    private PlanStageResponse planOf(int count, String goalPrefix) {
        return new PlanStageResponse(count, IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                        i, goalPrefix + "：第" + i + "章目标", "推进 " + i + "/" + count,
                        null, null, null, null, null))
                .toList());
    }

    private void stubWriter() {
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章",
                            "本章围绕目标「" + req.chapterGoal() + "」展开。",
                            "本章推进了：" + req.chapterGoal());
                });
    }

    private void stubExtractor() {
        when(aiServiceClient.extractMemory(any())).thenReturn(
                new com.example.storyai.ai.dto.ExtractMemoryResponse(java.util.List.of()));
    }

    private long createStage(Long storyId, int planCount) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(planCount, "目标"));
        stubWriter();
        stubExtractor();
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        // confirm plan so the stage is ACTIVE and generation is allowed
        mockMvc.perform(post("/api/stages/{id}/confirm", stageId)).andExpect(status().isOk());
        return stageId;
    }

    /** TASK-128: poll GET /generation-jobs/{id} until a terminal state is reached. */
    private JsonNode awaitTerminal(long jobId) throws Exception {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        JsonNode last = null;
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get("/api/generation-jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            last = objectMapper.readTree(body);
            String status = last.get("status").asText();
            if (List.of("COMPLETED", "FAILED", "STOPPED", "PAUSED").contains(status)) {
                return last;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("job " + jobId + " did not reach a terminal state within "
                + POLL_TIMEOUT_MS + "ms; last=" + last);
    }

    /** Polls until the job reports at least the given chapter progress. */
    private JsonNode awaitProgressAtLeast(long jobId, int minIndex) throws Exception {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get("/api/generation-jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
            if (node.get("currentPlanIndex").asInt() >= minIndex) {
                return node;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("job " + jobId + " never reached currentPlanIndex >= " + minIndex);
    }

    private static String normErr(JsonNode node) {
        return node.get("lastError").isNull() ? null : node.get("lastError").asText();
    }

    /**
     * After continue/retry the row may still show the PREVIOUS terminal state
     * (worker flips asynchronously). Waits for a terminal state that differs from
     * the snapshot by status, index, or lastError — never satisfied by a stale read.
     */
    private JsonNode awaitChangedTerminal(long jobId, JsonNode snapshot) throws Exception {
        String prevStatus = snapshot.get("status").asText();
        int prevIndex = snapshot.get("currentPlanIndex").asInt();
        String prevError = normErr(snapshot);
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get("/api/generation-jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(body);
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

    private long startJob(long stageId, String mode) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", mode))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private int countChapters(long stageId) throws Exception {
        String body = mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body).size();
    }

    // ---- AT-L02: Continuous mode runs to completion without per-chapter clicks ----

    @Test
    void continuousModeGeneratesAllChaptersToCompletion() throws Exception {
        Long storyId = createStory();
        long stageId = createStage(storyId, 3);

        long jobId = startJob(stageId, "CONTINUOUS");
        JsonNode job = awaitTerminal(jobId);

        org.assertj.core.api.Assertions.assertThat(job.get("status").asText()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(job.get("total").asInt()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(job.get("currentPlanIndex").asInt()).isEqualTo(3);

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ---- AT-L01: Step-by-Step mode pauses after each chapter ----

    @Test
    void stepByStepModePausesAfterEachChapter() throws Exception {
        Long storyId = createStory();
        long stageId = createStage(storyId, 3);

        // start: chapter 1 generated on the worker, then PAUSED
        long jobId = startJob(stageId, "STEP");
        JsonNode afterStart = awaitTerminal(jobId);
        org.assertj.core.api.Assertions.assertThat(afterStart.get("status").asText()).isEqualTo("PAUSED");
        org.assertj.core.api.Assertions.assertThat(countChapters(stageId)).isEqualTo(1);

        // continue -> chapter 2, still PAUSED
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterFirstContinue = awaitChangedTerminal(jobId, afterStart);
        org.assertj.core.api.Assertions.assertThat(afterFirstContinue.get("status").asText()).isEqualTo("PAUSED");
        org.assertj.core.api.Assertions.assertThat(countChapters(stageId)).isEqualTo(2);

        // continue -> chapter 3, now COMPLETED
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterSecondContinue = awaitChangedTerminal(jobId, afterFirstContinue);
        org.assertj.core.api.Assertions.assertThat(afterSecondContinue.get("status").asText()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(countChapters(stageId)).isEqualTo(3);
    }

    // ---- AT-M01/M02: writer failure marks job FAILED, retry recovers ----

    @Test
    void writerFailureMarksJobFailedAndRetryRecovers() throws Exception {
        Long storyId = createStory();
        long stageId = createStage(storyId, 3);
        // writer fails on the first generation call, then succeeds on retry.
        // registered AFTER createStage so it overrides createStage's default writer stub.
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenThrow(new AiServiceException("写作服务调用失败"))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse("第" + req.chapterOrder() + "章",
                            "内容", "摘要");
                });

        long jobId = startJob(stageId, "CONTINUOUS");
        JsonNode failed = awaitTerminal(jobId);
        org.assertj.core.api.Assertions.assertThat(failed.get("status").asText()).isEqualTo("FAILED");
        org.assertj.core.api.Assertions.assertThat(failed.get("lastError").asText())
                .contains("写作服务调用失败");

        // no chapter was persisted (failure before save)
        org.assertj.core.api.Assertions.assertThat(countChapters(stageId)).isZero();

        // retry: writer now succeeds, job runs to completion
        mockMvc.perform(post("/api/generation-jobs/{id}/retry", jobId))
                .andExpect(status().isOk());
        JsonNode recovered = awaitChangedTerminal(jobId, failed);
        org.assertj.core.api.Assertions.assertThat(recovered.get("status").asText()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(recovered.get("currentPlanIndex").asInt()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(countChapters(stageId)).isEqualTo(3);
    }
}
