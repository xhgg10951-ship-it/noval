package com.example.storyai.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
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

    @Autowired
    private com.example.storyai.context.StoryContextReader contextReader;

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

    private record GeneratedFixture(long stageId, long chapterId) {
    }

    private GeneratedFixture createActiveStageWithPlans(Long storyId, int planCount) throws Exception {
        return createActiveStageWithPlans(
                storyId,
                planCount,
                new GenerateChapterResponse("第一章", "AI 生成的原稿内容。", "原稿摘要。"));
    }

    private GeneratedFixture createActiveStageWithPlans(Long storyId,
                                                        int planCount,
                                                        GenerateChapterResponse firstChapter)
            throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(new PlanStageResponse(planCount, IntStream.rangeClosed(1, planCount)
                        .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                                i, "目标" + i, "推进", null, null, null, null, null))
                        .toList()));
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenReturn(firstChapter);
        when(aiServiceClient.summarizeChapter(any()))
                .thenAnswer(inv -> {
                    var req = inv.getArgument(0,
                            com.example.storyai.ai.dto.SummarizeChapterRequest.class);
                    return new com.example.storyai.ai.dto.SummarizeChapterResponse(
                            req.content());
                });
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
        long chapterId = objectMapper.readTree(generated.getResponse().getContentAsString())
                .get("id").asLong();
        return new GeneratedFixture(stageId, chapterId);
    }

    private long createActiveStageWithOneChapter(Long storyId) throws Exception {
        return createActiveStageWithPlans(storyId, 1).chapterId();
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

    // ---- RH-01 / AC-H01: the current read model must follow a manual revision ----

    @Test
    void manualEditRefreshesSummaryMemoryAndRecentWriterContext() throws Exception {
        Long storyId = createStory();
        GeneratedFixture fixture = createActiveStageWithPlans(
                storyId,
                2,
                new GenerateChapterResponse(
                        "铁剑入手", "主角在仓库中获得铁剑。", "主角获得铁剑。"));
        long chapterId = fixture.chapterId();

        // Reproduce the release blocker: the old body, summary and active memory
        // all say that the protagonist obtained an iron sword.
        jdbcTemplate.update("""
                INSERT INTO story_memory
                    (story_id, type, subject, description, source_chapter_id,
                     evidence, importance, scope, active)
                VALUES (?, 'PLOT_FACT', '铁剑', '主角获得铁剑', ?,
                        '主角在仓库中获得铁剑。', 5, 'STORY', 1)
                """, storyId, chapterId);
        clearInvocations(aiServiceClient);

        String editedBody = "主角检查空荡荡的仓库后，空手离开。";
        MvcResult edited = mockMvc.perform(put("/api/chapters/{id}/content", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("content", editedBody))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode current = objectMapper.readTree(
                edited.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(current.get("content").asText()).isEqualTo(editedBody);
        assertThat(current.get("summary").asText()).isNotBlank();
        assertThat(current.get("summary").asText()).contains("空手离开");
        assertThat(current.get("summary").asText()).doesNotContain("铁剑");
        assertThat(current.get("memoryExtractionStatus").asText()).isEqualTo("COMPLETED");

        Integer activeOldMemory = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM story_memory
                WHERE source_chapter_id = ? AND active = 1
                """, Integer.class, chapterId);
        assertThat(activeOldMemory).isZero();

        ArgumentCaptor<com.example.storyai.ai.dto.ExtractMemoryRequest> extraction =
                ArgumentCaptor.forClass(com.example.storyai.ai.dto.ExtractMemoryRequest.class);
        verify(aiServiceClient).extractMemory(extraction.capture());
        assertThat(extraction.getValue().chapterContent()).isEqualTo(editedBody);
        assertThat(extraction.getValue().chapterSummary()).contains("空手离开");
        assertThat(extraction.getValue().chapterSummary()).doesNotContain("铁剑");

        // ChapterGenerationService passes this value straight into the next
        // GenerateChapterRequest.recentContext; the deleted fact must be gone.
        String recentContext = contextReader.getRecentContextWithEnding(storyId, 3, 800);
        assertThat(recentContext).contains("空手离开");
        assertThat(recentContext).doesNotContain("铁剑");

        // Prove the actual next Writer request receives the refreshed context,
        // not merely that the context reader can compute it.
        clearInvocations(aiServiceClient);
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenReturn(new GenerateChapterResponse(
                        "第二章", "主角继续前行。", "主角继续前行。"));
        mockMvc.perform(post("/api/stages/{id}/chapters", fixture.stageId()))
                .andExpect(status().isOk());

        ArgumentCaptor<GenerateChapterRequest> nextWriter =
                ArgumentCaptor.forClass(GenerateChapterRequest.class);
        verify(aiServiceClient).generateChapter(nextWriter.capture());
        assertThat(nextWriter.getValue().recentContext()).contains("空手离开");
        assertThat(nextWriter.getValue().recentContext()).doesNotContain("铁剑");
        assertThat(nextWriter.getValue().storyMemories())
                .allSatisfy(memory -> assertThat(memory.description()).doesNotContain("铁剑"));
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

    // ---- v0.1.1 Phase 8 (TASK-167/168/169): polish workflow ----

    @Test
    void polishCreatesAiPolishRevisionAndRefreshesMemory() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);
        JsonNode before = getChapter(chapterId);

        when(aiServiceClient.polishChapter(any(com.example.storyai.ai.dto.PolishChapterRequest.class)))
                .thenReturn(new com.example.storyai.ai.dto.PolishChapterResponse(
                        "润色后的事实保持版本。"));
        clearInvocations(aiServiceClient);

        mockMvc.perform(post("/api/chapters/{id}/polish", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userInstruction\":\"对话更自然\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(before.get("id").asInt()))
                .andExpect(jsonPath("$.currentRevisionVersion").value(2))
                .andExpect(jsonPath("$.sourceType").value("AI_POLISH"))
                .andExpect(jsonPath("$.content").value("润色后的事实保持版本。"))
                .andExpect(jsonPath("$.title").value(before.get("title").asText()))
                .andExpect(jsonPath("$.summary").value("润色后的事实保持版本。"))
                .andExpect(jsonPath("$.memoryExtractionStatus").value("COMPLETED"));

        ArgumentCaptor<com.example.storyai.ai.dto.ExtractMemoryRequest> polishExtraction =
                ArgumentCaptor.forClass(com.example.storyai.ai.dto.ExtractMemoryRequest.class);
        verify(aiServiceClient).extractMemory(polishExtraction.capture());
        assertThat(polishExtraction.getValue().chapterSummary())
                .isEqualTo("润色后的事实保持版本。");

        // history: v1 AI_GENERATED preserved, v2 AI_POLISH current
        String revBody = mockMvc.perform(get("/api/chapters/{id}/revisions", chapterId))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode revisions = objectMapper.readTree(revBody);
        assertThat(revisions.size()).isEqualTo(2);
        assertThat(revisions.get(0).get("sourceType").asText()).isEqualTo("AI_POLISH");
        assertThat(revisions.get(1).get("sourceType").asText()).isEqualTo("AI_GENERATED");
        assertThat(revisions.get(1).get("content").asText()).contains("AI 生成的原稿");
    }

    // ---- TASK-144: regenerate same chapter -> same id, new AI_REWRITE revision ----

    @Test
    void regenerateKeepsChapterIdentityAndCreatesRewriteRevision() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);
        JsonNode before = getChapter(chapterId);

        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> new GenerateChapterResponse(
                        "重写后的标题", "重写后的全新正文。", "重写摘要。"));
        clearInvocations(aiServiceClient);

        mockMvc.perform(post("/api/chapters/{id}/regenerate", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorInstruction\":\"节奏更紧凑\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(before.get("id").asInt()))
                .andExpect(jsonPath("$.chapterNumber").value(before.get("chapterNumber").asInt()))
                .andExpect(jsonPath("$.currentRevisionVersion").value(2))
                .andExpect(jsonPath("$.sourceType").value("AI_REWRITE"))
                .andExpect(jsonPath("$.title").value("重写后的标题"))
                .andExpect(jsonPath("$.content").value("重写后的全新正文。"))
                .andExpect(jsonPath("$.summary").value("重写摘要。"))
                // TASK-148 loop closed inside regenerate: memory re-extracted to COMPLETED
                .andExpect(jsonPath("$.memoryExtractionStatus").value("COMPLETED"));

        ArgumentCaptor<com.example.storyai.ai.dto.ExtractMemoryRequest> rewriteExtraction =
                ArgumentCaptor.forClass(com.example.storyai.ai.dto.ExtractMemoryRequest.class);
        verify(aiServiceClient).extractMemory(rewriteExtraction.capture());
        assertThat(rewriteExtraction.getValue().chapterSummary()).isEqualTo("重写摘要。");

        // history intact: v1 (AI_GENERATED) still readable, v2 (AI_REWRITE) current
        String revBody = mockMvc.perform(get("/api/chapters/{id}/revisions", chapterId))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode revisions = objectMapper.readTree(revBody);
        assertThat(revisions.size()).isEqualTo(2);
        assertThat(revisions.get(0).get("sourceType").asText()).isEqualTo("AI_REWRITE");
        assertThat(revisions.get(1).get("sourceType").asText()).isEqualTo("AI_GENERATED");
        assertThat(revisions.get(1).get("content").asText()).contains("AI 生成的原稿");
    }

    // ---- TASK-147/148: manual edit marks STALE; re-extract invalidates derived memories ----

    @Test
    void manualEditMarksMemoryStaleAndReextractInvalidatesDerivedMemories() throws Exception {
        Long storyId = createStory();
        long chapterId = createActiveStageWithOneChapter(storyId);

        // simulate a previously derived + applied memory from this chapter
        jdbcTemplate.update("""
                INSERT INTO story_memory (story_id, type, subject, description, source_chapter_id, evidence)
                VALUES (?, 'EVENT', '旧事实', '旧正文派生的事实', ?, 'evidence')
                """, createdStoryIds.get(0), chapterId);

        // RH-01: the public Manual Edit workflow must close STALE -> re-extract
        // before returning, rather than requiring a hidden service call.
        mockMvc.perform(put("/api/chapters/{id}/content", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"作者修改后的正文，事实已变化。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memoryExtractionStatus").value("COMPLETED"));

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM story_memory WHERE source_chapter_id = ?",
                Integer.class, chapterId);
        assertThat(remaining).isZero();

        JsonNode after = getChapter(chapterId);
        assertThat(after.get("memoryExtractionStatus").asText()).isEqualTo("COMPLETED");
        // content untouched by re-extraction (only memories were refreshed)
        assertThat(after.get("content").asText()).contains("作者修改后的正文");
    }
}
