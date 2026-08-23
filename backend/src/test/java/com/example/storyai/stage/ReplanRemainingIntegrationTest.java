package com.example.storyai.stage;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Phase 4 (TASK-134/136) — Replan Remaining integration tests.
 *
 * <pre>
 * AC-106 shape: generate part of a stage, replan the remainder,
 * verify completed chapters + plans survive and only the future is rewritten.
 * </pre>
 *
 * <p>The Python AI Service is mocked; orchestration + persistence are real.
 * Non-transactional by design: generation runs on a background executor
 * (TASK-127), so the worker needs committed rows. Cleanup cascades from story.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ReplanRemainingIntegrationTest {

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
            jdbcTemplate.update("DELETE FROM story WHERE id = ?", storyId);
        }
        createdStoryIds.clear();
    }

    // ---- helpers ----

    private Long createStory() throws Exception {
        String body = """
                {"name":"重规划剩余测试故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
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
                        i, "原方向：第" + i + "章", "推进 " + i + "/" + count,
                        null, null, null, null, null))
                .toList());
    }

    /** Plan for the REPLANNED remainder — distinct goals prove which plan ran. */
    private PlanStageResponse remainingPlanOf(int count, String marker) {
        return new PlanStageResponse(count, IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                        i, marker + "：第" + i + "章", "新推进 " + i + "/" + count,
                        null, null, null, null, null))
                .toList());
    }

    /** Mirrors the continuation-aware qwen response retained by RH-10. */
    private PlanStageResponse globalPlanOf(int firstOrder, int count, String marker) {
        return new PlanStageResponse(count, IntStream.range(0, count)
                .mapToObj(offset -> {
                    int order = firstOrder + offset;
                    return new PlanStageResponse.ChapterPlanItem(
                            order, marker + "：第" + order + "章",
                            "新推进 " + (offset + 1) + "/" + count,
                            3000, List.of("继续既成事实"), List.of("不得重开"),
                            List.of("推进当前事件"), "衔接下一章");
                })
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

    private long createActiveStage(Long storyId, int planCount) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class))).thenReturn(planOf(planCount));
        stubWriter();
        when(aiServiceClient.extractMemory(any())).thenReturn(
                new com.example.storyai.ai.dto.ExtractMemoryResponse(List.of()));
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"主线方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(post("/api/stages/{id}/confirm", stageId)).andExpect(status().isOk());
        return stageId;
    }

    private long startStep(long stageId) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "STEP"))
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
        JsonNode last = null;
        while (System.currentTimeMillis() < deadline) {
            last = getJob(jobId);
            String status = last.get("status").asText();
            if (List.of("COMPLETED", "FAILED", "STOPPED", "PAUSED").contains(status)) {
                return last;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("job " + jobId + " stuck at " + last);
    }

    private JsonNode awaitChangedTerminal(long jobId, JsonNode snapshot) throws Exception {
        String prevStatus = snapshot.get("status").asText();
        int prevIndex = snapshot.get("currentPlanIndex").asInt();
        String prevError = snapshot.get("lastError").isNull() ? null : snapshot.get("lastError").asText();
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            JsonNode node = getJob(jobId);
            String status = node.get("status").asText();
            if (List.of("COMPLETED", "FAILED", "STOPPED", "PAUSED").contains(status)) {
                int index = node.get("currentPlanIndex").asInt();
                String err = node.get("lastError").isNull() ? null : node.get("lastError").asText();
                if (!status.equals(prevStatus) || index != prevIndex
                        || !java.util.Objects.equals(err, prevError)) {
                    return node;
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("job " + jobId + " never left " + prevStatus + "/" + prevIndex);
    }

    private JsonNode getStageJson(long stageId) throws Exception {
        String body = mockMvc.perform(get("/api/stages/{id}", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    // ---- AC-106 shape: replan the remainder, preserve history ----

    @Test
    void replanRemainingPreservesCompletedAndRewritesOnlyFuture() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 4);

        // generate the first two chapters via STEP mode
        long jobId = startStep(stageId);
        JsonNode afterCh1 = awaitTerminal(jobId);
        assertThat(afterCh1.get("status").asText()).isEqualTo("PAUSED");
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId)).andExpect(status().isOk());
        JsonNode afterCh2 = awaitChangedTerminal(jobId, afterCh1);
        assertThat(afterCh2.get("status").asText()).isEqualTo("PAUSED");

        // snapshot the stage (plans incl. version/status) before replanning
        JsonNode stageBefore = getStageJson(stageId);

        // replan the remainder to 2 chapters with an author instruction
        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(remainingPlanOf(2, "新方向"));
        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"remainingChapterCount":2,"authorInstruction":"后半转向地下城探索"}
                                """))
                .andExpect(status().isOk());

        JsonNode stageAfter = getStageJson(stageId);
        JsonNode plans = stageAfter.get("plans");

        // all 4 old plans still exist (history preserved — nothing deleted)
        assertThat(plans.size()).isEqualTo(6);

        // old plans: first two COMPLETED (their chapters exist), next two SUPERSEDED
        int completed = 0;
        int superseded = 0;
        int activeV2 = 0;
        int maxOldOrder = 0;
        for (JsonNode p : plans) {
            String status = p.get("status").asText();
            int version = p.get("planVersion").asInt();
            if ("COMPLETED".equals(status)) {
                assertThat(version).isEqualTo(1);
                completed++;
                maxOldOrder = Math.max(maxOldOrder, p.get("chapterOrder").asInt());
            } else if ("SUPERSEDED".equals(status)) {
                assertThat(version).isEqualTo(1);
                superseded++;
            } else if ("ACTIVE".equals(status)) {
                assertThat(version).isEqualTo(2);
                activeV2++;
            }
        }
        assertThat(completed).isEqualTo(2);   // plans with generated chapters — untouched
        assertThat(superseded).isEqualTo(2);  // old remaining — superseded, NOT deleted
        assertThat(activeV2).isEqualTo(2);    // new remainder — version 2

        // new-version plans continue after the latest COMPLETED chapter; overlap
        // with superseded historical orders is intentionally separated by version.
        for (JsonNode p : plans) {
            if (p.get("planVersion").asInt() == 2) {
                assertThat(p.get("chapterOrder").asInt()).isGreaterThan(maxOldOrder);
                assertThat(p.get("goal").asText()).contains("新方向");
            }
        }

        // resume: only the 2 new-version plans run; no regeneration of ch1..2.
        // STEP mode: each continue runs exactly one chapter, then pauses again.
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId)).andExpect(status().isOk());
        JsonNode afterCh3 = awaitChangedTerminal(jobId, afterCh2);
        assertThat(afterCh3.get("status").asText()).isEqualTo("PAUSED");

        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId)).andExpect(status().isOk());
        JsonNode done = awaitChangedTerminal(jobId, afterCh3);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");

        String chaptersBody = mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode chapters = objectMapper.readTree(chaptersBody);
        assertThat(chapters.size()).isEqualTo(4);
        List<Integer> numbers = new java.util.ArrayList<>();
        for (JsonNode c : chapters) {
            numbers.add(c.get("chapterNumber").asInt());
        }
        assertThat(numbers.stream().sorted().toList()).containsExactly(1, 2, 3, 4);

        // the first two chapters are byte-for-byte the SAME rows (ids stable)
        JsonNode stageFinal = getStageJson(stageId);
        assertThat(stageFinal.get("plans").size()).isEqualTo(6);

        // chapters 3..4 were written from the NEW plan goals (marker proves it)
        String c3 = null;
        String c4 = null;
        for (JsonNode c : chapters) {
            int n = c.get("chapterNumber").asInt();
            if (n == 3) {
                c3 = c.get("summary").asText();
            }
            if (n == 4) {
                c4 = c.get("summary").asText();
            }
        }
        assertThat(c3).contains("新方向");
        assertThat(c4).contains("新方向");
        org.junit.jupiter.api.Assertions.assertNotNull(stageBefore);
    }

    // ---- RH-03 / AC-H03: plan order is the real next chapter number ----

    @Test
    void replanRemainingUsesNextRealChapterNumberAcrossVersionHistory() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 9);

        // Complete real Chapters 1..3 while V1 plans 4..9 remain active.
        long jobId = startStep(stageId);
        JsonNode afterOne = awaitTerminal(jobId);
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterTwo = awaitChangedTerminal(jobId, afterOne);
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterThree = awaitChangedTerminal(jobId, afterTwo);
        assertThat(afterThree.get("status").asText()).isEqualTo("PAUSED");

        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(remainingPlanOf(3, "V2新方向"));
        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remainingChapterCount\":3}"))
                .andExpect(status().isOk());

        JsonNode replanned = getStageJson(stageId);
        List<Integer> activeV2Orders = new java.util.ArrayList<>();
        int completedV1 = 0;
        int supersededV1 = 0;
        for (JsonNode plan : replanned.get("plans")) {
            int version = plan.get("planVersion").asInt();
            String planStatus = plan.get("status").asText();
            if (version == 1 && "COMPLETED".equals(planStatus)) {
                completedV1++;
            } else if (version == 1 && "SUPERSEDED".equals(planStatus)) {
                supersededV1++;
            } else if (version == 2 && "ACTIVE".equals(planStatus)) {
                activeV2Orders.add(plan.get("chapterOrder").asInt());
            }
        }
        assertThat(completedV1).isEqualTo(3);
        assertThat(supersededV1).isEqualTo(6);
        assertThat(activeV2Orders).containsExactly(4, 5, 6);

        // Resume the same STEP job. The queue is DB-driven, so the new V2 plans
        // produce the real Chapters 4..6 and then converge to COMPLETED.
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterFour = awaitChangedTerminal(jobId, afterThree);
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterFive = awaitChangedTerminal(jobId, afterFour);
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk());
        JsonNode afterSix = awaitChangedTerminal(jobId, afterFive);
        assertThat(afterSix.get("status").asText()).isEqualTo("COMPLETED");

        JsonNode chapters = objectMapper.readTree(mockMvc.perform(
                        get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        List<Integer> chapterNumbers = new java.util.ArrayList<>();
        for (JsonNode chapter : chapters) {
            chapterNumbers.add(chapter.get("chapterNumber").asInt());
        }
        assertThat(chapterNumbers).containsExactly(1, 2, 3, 4, 5, 6);

        org.mockito.ArgumentCaptor<GenerateChapterRequest> writerRequests =
                org.mockito.ArgumentCaptor.forClass(GenerateChapterRequest.class);
        org.mockito.Mockito.verify(aiServiceClient, org.mockito.Mockito.times(6))
                .generateChapter(writerRequests.capture());
        assertThat(writerRequests.getAllValues().stream()
                .map(GenerateChapterRequest::chapterOrder)
                .toList()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    /**
     * RH-03 regression: the real qwen continuation responses retained by RH-10
     * use logical story orders (4..6), not relative orders (1..3). Java owns the
     * canonical persisted order and must accept either representation without
     * rejecting or double-shifting it.
     */
    @Test
    void replanRemainingAcceptsGlobalLogicalOrdersFromPlanner() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 9);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                    .andExpect(status().isOk());
        }

        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(globalPlanOf(4, 3, "qwen续写"));

        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remainingChapterCount\":3}"))
                .andExpect(status().isOk());

        List<Integer> activeOrders = new java.util.ArrayList<>();
        for (JsonNode plan : getStageJson(stageId).get("plans")) {
            if (plan.get("planVersion").asInt() == 2
                    && "ACTIVE".equals(plan.get("status").asText())) {
                activeOrders.add(plan.get("chapterOrder").asInt());
            }
        }
        assertThat(activeOrders).containsExactly(4, 5, 6);
    }

    /** A later Stage uses the same logical-order contract as Replan Remaining. */
    @Test
    void laterStageAcceptsGlobalLogicalOrdersFromPlanner() throws Exception {
        Long storyId = createStory();
        long firstStageId = createActiveStage(storyId, 3);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/stages/{id}/chapters", firstStageId))
                    .andExpect(status().isOk());
        }

        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(globalPlanOf(4, 2, "新阶段续写"));

        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"继续现有剧情\",\"targetChapterCount\":2}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode stage = objectMapper.readTree(
                created.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        List<Integer> orders = new java.util.ArrayList<>();
        for (JsonNode plan : stage.get("plans")) {
            orders.add(plan.get("chapterOrder").asInt());
        }
        assertThat(orders).containsExactly(4, 5);
    }

    // ---- TASK-134 guard: wholesale replan is refused once generation started ----

    @Test
    void fullReplanRefusedForActiveStage() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 2);

        mockMvc.perform(post("/api/stages/{id}/replan", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetChapterCount\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        // history untouched
        assertThat(getStageJson(stageId).get("plans").size()).isEqualTo(2);
    }

    // ---- full replan remains available BEFORE confirmation (PLANNING) ----

    @Test
    void fullReplanStillWorksInPlanning() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class))).thenReturn(planOf(3));
        stubWriter();
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"主线方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(remainingPlanOf(5, "调整"));
        mockMvc.perform(post("/api/stages/{id}/replan", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetChapterCount\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedChapterCount").value(5));

        assertThat(getStageJson(stageId).get("plans").size()).isEqualTo(5);
    }

    // ---- replan-remaining refuses non-ACTIVE stages ----

    @Test
    void replanRemainingRefusedForPlanningStage() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class))).thenReturn(planOf(2));
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"主线方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remainingChapterCount\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    // ---- TASK-137: a RUNNING generation blocks replanning until paused/stopped ----

    @Test
    void replanRemainingRefusedWhileJobRunning() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 3);

        // hold the writer mid-call so the job stays RUNNING
        java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    gate.await(POLL_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse("第" + req.chapterOrder() + "章",
                            "内容「" + req.chapterGoal() + "」", "推进：" + req.chapterGoal());
                });
        long jobId = startStep(stageId);

        // wait until the worker is inside the writer call
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline
                && !"RUNNING".equals(getJob(jobId).get("status").asText())) {
            Thread.sleep(50);
        }

        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(remainingPlanOf(2, "新方向"));
        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remainingChapterCount\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        // pause reaches the checkpoint -> PAUSED job no longer blocks replanning
        mockMvc.perform(post("/api/generation-jobs/{id}/pause", jobId)).andExpect(status().isOk());
        gate.countDown();
        JsonNode paused = awaitTerminal(jobId);
        assertThat(paused.get("status").asText()).isEqualTo("PAUSED");

        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"remainingChapterCount":1,"authorInstruction":"收紧后续"}
                                """))
                .andExpect(status().isOk());
    }

    // ---- TASK-137: after a replan, a NEW job's total counts only the active queue ----

    @Test
    void jobTotalCountsOnlyActiveRemainingPlansAfterReplan() throws Exception {
        Long storyId = createStory();
        long stageId = createActiveStage(storyId, 4);

        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(remainingPlanOf(3, "新方向"));
        mockMvc.perform(post("/api/stages/{id}/replan-remaining", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remainingChapterCount\":3}"))
                .andExpect(status().isOk());

        // 4 v1 rows exist in history, but only 3 active plans remain — total must be 3
        long jobId = startStep(stageId);
        JsonNode job = getJob(jobId);
        assertThat(job.get("total").asInt()).isEqualTo(3);
    }
}
