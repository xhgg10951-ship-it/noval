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
import com.example.storyai.memory.model.StoryMemory;
import com.example.storyai.memory.service.MemoryService;
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

    @Autowired
    private MemoryService memoryService;

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
                        i, goalPrefix + "：第" + i + "章目标", "推进 " + i + "/" + count,
                        null, null, null, null, null))
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

    private void saveMemory(long storyId, String type, String subject,
                            String description, int importance, String scope) {
        StoryMemory memory = new StoryMemory();
        memory.setStoryId(storyId);
        memory.setType(type);
        memory.setSubject(subject);
        memory.setDescription(description);
        memory.setEvidence(description);
        memory.setImportance(importance);
        memory.setScope(scope);
        memory.setActive(true);
        memoryService.saveStoryMemory(memory);
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

    // ---- RH-05 / HH-001: Writer memory is relevant to Arc + Stage + ChapterSpec ----

    @Test
    void writerMemorySelectionUsesArcStageAndChapterSpecBeforeHardCap() throws Exception {
        Long storyId = createStory();
        PlanStageResponse richPlan = new PlanStageResponse(1, java.util.List.of(
                new PlanStageResponse.ChapterPlanItem(
                        1,
                        "艾琳在北境遗迹检查玉佩",
                        "确认玉佩与月蚀教团的关联",
                        3200,
                        java.util.List.of("艾琳必须找到玉佩上的月蚀印记"),
                        java.util.List.of("不得离开北境遗迹"),
                        java.util.List.of("艾琳进入遗迹", "取出玉佩", "辨认月蚀印记"),
                        "发现月蚀教团留下的新线索")));
        when(aiServiceClient.planStage(any(PlanStageRequest.class))).thenReturn(richPlan);
        stubWriter();
        stubExtractor();

        mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"月蚀卷","goal":"追查月蚀教团",\
                                 "targetStartChapter":1,"targetEndChapter":20,"status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated());

        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"前往北境遗迹追踪月蚀教团\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(post("/api/stages/{id}/confirm", stageId)).andExpect(status().isOk());

        saveMemory(storyId, "PLOT_FACT", "世界核心", "世界曾被两轮月亮照耀", 5, "STORY");
        saveMemory(storyId, "PLOT_THREAD", "失踪信使", "信使仍未找到", 4, "ARC");
        for (int i = 0; i < 22; i++) {
            saveMemory(storyId, "PLOT_FACT", "无关人物" + i,
                    "无关旧闻" + i, 3, "STORY");
        }
        // These arrive after more than twenty eligible rows. The old
        // importance-only selector drops all of them before the Writer call.
        saveMemory(storyId, "PLOT_FACT", "艾琳", "艾琳能辨认玉佩上的古代文字", 3, "STAGE");
        saveMemory(storyId, "WORLD_RULE", "北境遗迹", "遗迹入口只在月光下开启", 3, "STAGE");
        saveMemory(storyId, "FORESHADOWING", "月蚀教团", "教团在玉佩上留下月蚀印记", 3, "ARC");

        mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk());

        ArgumentCaptor<GenerateChapterRequest> captor =
                ArgumentCaptor.forClass(GenerateChapterRequest.class);
        verify(aiServiceClient).generateChapter(captor.capture());
        var selected = captor.getValue().storyMemories();
        assertThat(selected).hasSizeLessThanOrEqualTo(20);
        assertThat(selected).extracting(GenerateChapterRequest.MemoryItem::description)
                .contains(
                        "世界曾被两轮月亮照耀",
                        "信使仍未找到",
                        "艾琳能辨认玉佩上的古代文字",
                        "遗迹入口只在月光下开启",
                        "教团在玉佩上留下月蚀印记");
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
