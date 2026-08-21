package com.example.storyai.chapter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.ai.AiServiceClient;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.common.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for multi-chapter generation modes (M5 / TASK-036..040).
 * Covers AT-L01 (Step-by-Step), AT-L02 (Continuous), AT-M01/M02 (failure + retry).
 * The Python AI Service is mocked; only the orchestration and persistence are real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class GenerationModesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private Long createStory() throws Exception {
        String body = """
                {"name":"多章生成测试故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
                """;
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private PlanStageResponse planOf(int count, String goalPrefix) {
        return new PlanStageResponse(count, IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                        i, goalPrefix + "：第" + i + "章目标", "推进 " + i + "/" + count))
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

    // ---- AT-L02: Continuous mode runs to completion without per-chapter clicks ----

    @Test
    void continuousModeGeneratesAllChaptersToCompletion() throws Exception {
        Long storyId = createStory();
        long stageId = createStage(storyId, 3);

        mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "CONTINUOUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentPlanIndex").value(3))
                .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ---- AT-L01: Step-by-Step mode pauses after each chapter ----

    @Test
    void stepByStepModePausesAfterEachChapter() throws Exception {
        Long storyId = createStory();
        long stageId = createStage(storyId, 3);

        // start: only chapter 1 generated, then PAUSED
        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "STEP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentPlanIndex").value(1))
                .andReturn();
        long jobId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();

        // continue -> chapter 2, still PAUSED
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.currentPlanIndex").value(2));

        // continue -> chapter 3, now COMPLETED
        mockMvc.perform(post("/api/generation-jobs/{id}/continue", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentPlanIndex").value(3));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ---- AT-M01/M02: writer failure marks job FAILED, retry recovers ----

    @Test
    void writerFailureMarksJobFailedAndRetryRecovers() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "目标"));
        stubExtractor();
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

        MvcResult started = mockMvc.perform(post("/api/stages/{id}/generate", stageId)
                        .param("mode", "CONTINUOUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.lastError").value(containsString("写作服务调用失败")))
                .andReturn();
        long jobId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("id").asLong();

        // no chapter was persisted (failure before save)
        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // retry: writer now succeeds, job runs to completion
        mockMvc.perform(post("/api/generation-jobs/{id}/retry", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentPlanIndex").value(3));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
