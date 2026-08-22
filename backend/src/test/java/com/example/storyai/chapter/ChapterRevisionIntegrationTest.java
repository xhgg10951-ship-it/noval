package com.example.storyai.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * v0.1.1 Phase 5 — Chapter Revision integration tests (TASK-140..146).
 *
 * <pre>
 * generation creates AI_GENERATED revision #1, chapter stays DRAFT
 * manual edit creates a NEW revision; the old one survives (no overwrite)
 * approve flips DRAFT -> APPROVED; a further edit re-opens DRAFT
 * revision history lists newest first with version + source type
 * </pre>
 *
 * <p>AI mocked; persistence real. Cleanup cascades from story.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ChapterRevisionIntegrationTest {

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
                {"name":"修订测试故事","coreIdea":"主角在异世界逐步成长","constraints":[]}
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

    private long createActiveStageWithOneChapter(Long storyId) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(new PlanStageResponse(1, IntStream.rangeClosed(1, 1)
                        .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                                i, "目标" + i, "推进", null, null, null, null, null))
                        .toList()));
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> new GenerateChapterResponse(
                        "第一章", "AI 生成的原稿内容。", "原稿摘要。"));
        when(aiServiceClient.extractMemory(any())).thenReturn(
                new com.example.storyai.ai.dto.ExtractMemoryResponse(List.of()));

        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(post("/api/stages/{id}/confirm", stageId)).andExpect(status().isOk());

        MvcResult generated = mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(generated.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private JsonNode getChapter(long chapterId) throws Exception {
        String body = mockMvc.perform(get("/api/chapters/{id}", chapterId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    // ---- TASK-140/141: generation lands as AI_GENERATED revision #1, DRAFT ----

    @Test
    void generatedChapterBecomesAiGeneratedRevisionAndStaysDraft() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);

        JsonNode chapter = getChapter(chapterId);
        assertThat(chapter.get("status").asText()).isEqualTo("DRAFT");
        assertThat(chapter.get("currentRevisionVersion").asInt()).isEqualTo(1);
        assertThat(chapter.get("sourceType").asText()).isEqualTo("AI_GENERATED");

        // exactly one revision exists and it holds the same content
        String revBody = mockMvc.perform(get("/api/chapters/{id}/revisions", chapterId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode revisions = objectMapper.readTree(revBody);
        assertThat(revisions.size()).isEqualTo(1);
        assertThat(revisions.get(0).get("versionNumber").asInt()).isEqualTo(1);
        assertThat(revisions.get(0).get("sourceType").asText()).isEqualTo("AI_GENERATED");
        assertThat(revisions.get(0).get("content").asText()).contains("AI 生成的原稿");
    }

    // ---- TASK-142: manual edit creates a NEW revision, old one survives ----

    @Test
    void manualEditCreatesNewRevisionWithoutOverwritingHistory() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);

        mockMvc.perform(put("/api/chapters/{id}/content", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"作者亲笔修改后的正文。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRevisionVersion").value(2))
                .andExpect(jsonPath("$.sourceType").value("MANUAL_EDIT"))
                .andExpect(jsonPath("$.content").value("作者亲笔修改后的正文。"));

        String revBody = mockMvc.perform(get("/api/chapters/{id}/revisions", chapterId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode revisions = objectMapper.readTree(revBody);
        assertThat(revisions.size()).isEqualTo(2);
        // newest first
        assertThat(revisions.get(0).get("versionNumber").asInt()).isEqualTo(2);
        assertThat(revisions.get(0).get("sourceType").asText()).isEqualTo("MANUAL_EDIT");
        assertThat(revisions.get(1).get("versionNumber").asInt()).isEqualTo(1);
        assertThat(revisions.get(1).get("sourceType").asText()).isEqualTo("AI_GENERATED");
        // the ORIGINAL prose still lives in revision #1 — history is intact
        assertThat(revisions.get(1).get("content").asText()).contains("AI 生成的原稿");
    }

    // ---- TASK-146 (+TASK-142 interplay): approve, then edit re-opens DRAFT ----

    @Test
    void approveFlipsToApprovedAndFurtherEditsReopenDraft() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);

        mockMvc.perform(post("/api/chapters/{id}/approve", chapterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.currentRevisionVersion").value(1));

        // idempotent approve keeps APPROVED
        mockMvc.perform(post("/api/chapters/{id}/approve", chapterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // a new write re-opens DRAFT (the author is reviewing again);
        // approving does NOT create a revision, so this edit is version 2
        mockMvc.perform(put("/api/chapters/{id}/content", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"批准后又改了一版。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentRevisionVersion").value(2));
    }

    // ---- 404s for unknown chapter / revision ----

    @Test
    void unknownChapterReturns404() throws Exception {
        mockMvc.perform(get("/api/chapters/999999/revisions"))
                .andExpect(status().isNotFound());
    }
}
