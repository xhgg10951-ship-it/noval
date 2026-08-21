package com.example.storyai.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the Single Chapter Generation vertical slice (TASK-018..024, AT-C01).
 *
 * <p>The Python AI Service is {@link MockitoBean @MockitoBean}ed so the tests run
 * offline; the writer contract (request carries story/stage/plan context, structured
 * title/content/summary response out) is exactly what the real service implements.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ChapterGenerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    // ---- helpers ----

    private Long createStory() throws Exception {
        String body = """
                {"name":"章节生成测试故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
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

    /** Writer stub that embeds the plan goal into the chapter so we can prove AT-C01. */
    private void stubWriter() {
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章",
                            "本章围绕目标「" + req.chapterGoal() + "」展开，主角继续推进剧情。",
                            "本章推进了：" + req.chapterGoal());
                });
    }

    /** Stub the memory extractor so the M4 generate->extract wiring is a no-op in these chapter tests. */
    private void stubExtractor() {
        when(aiServiceClient.extractMemory(any())).thenReturn(
                new com.example.storyai.ai.dto.ExtractMemoryResponse(java.util.List.of()));
    }

    // ---- AT-C01: generate next chapter persists and advances the plan goal ----

    @Test
    void generateNextChapterPersistsAndAdvancesGoal() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "目标"));
        stubWriter();

        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"主角和艾琳前往公会\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        stubExtractor();
        mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterNumber").value(1))
                .andExpect(jsonPath("$.title").value("第1章"))
                .andExpect(jsonPath("$.content").value(containsString("目标：第1章目标")))
                .andExpect(jsonPath("$.generationStatus").value("GENERATED"));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Context assembly: the writer request carried the full story/stage/plan context.
        ArgumentCaptor<GenerateChapterRequest> captor =
                ArgumentCaptor.forClass(GenerateChapterRequest.class);
        verify(aiServiceClient).generateChapter(captor.capture());
        GenerateChapterRequest sent = captor.getValue();
        assertThat(sent.coreIdea()).isEqualTo("主角在异世界逐步成长");
        assertThat(sent.stageDirection()).isEqualTo("主角和艾琳前往公会");
        assertThat(sent.constraints()).hasSize(1);
        assertThat(sent.chapterGoal()).contains("第1章目标");
        assertThat(sent.chapterOrder()).isEqualTo(1);
    }

    // ---- next chapter picks the next pending plan (order 2) ----

    @Test
    void secondChapterPicksNextPendingPlan() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "目标"));
        stubWriter();
        stubExtractor();

        long stageId = objectMapper.readTree(mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/stages/{id}/chapters", stageId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapterNumber").value(2));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- 409 once every plan already has a chapter ----

    @Test
    void noPendingChapterReturns409() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(2, "目标"));
        stubWriter();
        stubExtractor();

        long stageId = objectMapper.readTree(mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/stages/{id}/chapters", stageId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/stages/{id}/chapters", stageId)).andExpect(status().isOk());
        // all 2 plans generated -> next call is a conflict
        mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_PENDING_CHAPTER"));

        mockMvc.perform(get("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- invalid writer response (missing content) -> 502 AI_SERVICE_ERROR ----

    @Test
    void invalidWriterResponseYields502() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "目标"));
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenReturn(new GenerateChapterResponse("标题", "", "摘要")); // blank content
        stubExtractor();

        long stageId = objectMapper.readTree(mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
    }
}
