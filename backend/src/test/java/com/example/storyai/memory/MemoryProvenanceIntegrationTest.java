package com.example.storyai.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
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
import com.example.storyai.ai.dto.ExtractMemoryRequest;
import com.example.storyai.ai.dto.ExtractMemoryResponse;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.example.storyai.ai.dto.SummarizeChapterRequest;
import com.example.storyai.ai.dto.SummarizeChapterResponse;
import com.example.storyai.memory.model.CurrentState;
import com.example.storyai.memory.model.RelationshipState;
import com.example.storyai.memory.service.MemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

/** RH-02 / AC-H02 regression coverage for live-state provenance. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD", matches = ".+")
class MemoryProvenanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemoryService memoryService;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @Test
    void revisingSwordChapterRemovesNormalizedInventorySlot() throws Exception {
        long storyId = createStory("inventory provenance");
        long stageId = createStage(storyId, 1);

        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenReturn(new GenerateChapterResponse(
                        "获得铁剑", "林夜在仓库中获得铁剑。", "林夜获得铁剑。"));
        when(aiServiceClient.extractMemory(any(ExtractMemoryRequest.class)))
                .thenAnswer(invocation -> {
                    ExtractMemoryRequest request = invocation.getArgument(0);
                    if (request.chapterContent().contains("铁剑")) {
                        return extracted(candidate(
                                "CURRENT_STATE", "林夜", "inventory", "铁剑"));
                    }
                    return extracted();
                });
        stubSummaryRefresh();

        long chapterId = generateChapter(stageId);
        assertThat(memoryService.getCurrentState(storyId))
                .anySatisfy(state -> {
                    assertThat(state.getCategory()).isEqualTo("INVENTORY");
                    assertThat(state.getField()).isEqualTo("item:铁剑");
                    assertThat(state.getSourceCandidateId()).isNotNull();
                });

        editChapter(chapterId, "林夜检查空仓库后，空手离开。");

        assertThat(memoryService.getCurrentState(storyId))
                .noneSatisfy(state -> assertThat(state.getField()).isEqualTo("item:铁剑"));
    }

    @Test
    void revisingOlderChapterDoesNotDeleteNewerStateOrRelationship() throws Exception {
        long storyId = createStory("newest provenance wins");
        long stageId = createStage(storyId, 2);

        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(invocation -> {
                    GenerateChapterRequest request = invocation.getArgument(0);
                    if (request.chapterOrder() == 1) {
                        return new GenerateChapterResponse(
                                "酒馆相遇",
                                "林夜身在酒馆，艾琳仍对林夜保持戒备。",
                                "林夜在酒馆遇见戒备的艾琳。");
                    }
                    return new GenerateChapterResponse(
                            "抵达公会",
                            "林夜抵达公会，艾琳开始信任林夜。",
                            "林夜在公会赢得艾琳信任。");
                });
        when(aiServiceClient.extractMemory(any(ExtractMemoryRequest.class)))
                .thenAnswer(invocation -> {
                    String content = invocation.getArgument(0, ExtractMemoryRequest.class)
                            .chapterContent();
                    if (content.contains("酒馆")) {
                        return extracted(
                                candidate("CURRENT_STATE", "林夜", "location", "酒馆"),
                                candidate("RELATIONSHIP", "艾琳->林夜", null, "保持戒备"));
                    }
                    if (content.contains("公会")) {
                        return extracted(
                                candidate("CURRENT_STATE", "林夜", "location", "公会"),
                                candidate("RELATIONSHIP", "艾琳->林夜", null, "开始信任"));
                    }
                    return extracted();
                });
        stubSummaryRefresh();

        long chapterOneId = generateChapter(stageId);
        generateChapter(stageId);

        assertCurrentLocation(storyId, "公会");
        assertRelationship(storyId, "开始信任");

        editChapter(chapterOneId, "林夜只在城外短暂停留。");

        assertCurrentLocation(storyId, "公会");
        assertRelationship(storyId, "开始信任");
    }

    @Test
    void revisionSupersedesPendingCandidateSoDeletedFactCannotBeAppliedLater() throws Exception {
        long storyId = createStory("pending candidate invalidation");
        long stageId = createStage(storyId, 1);
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenReturn(new GenerateChapterResponse(
                        "旧线索", "林夜发现旧线索刻在墙上。", "林夜发现旧线索。"));
        when(aiServiceClient.extractMemory(any(ExtractMemoryRequest.class)))
                .thenAnswer(invocation -> {
                    String content = invocation.getArgument(0, ExtractMemoryRequest.class)
                            .chapterContent();
                    if (content.contains("旧线索")) {
                        return extracted(new ExtractMemoryResponse.MemoryCandidate(
                                "PLOT_FACT", "旧线索", null, "墙上刻有旧线索",
                                "REVIEW", "旧线索刻在墙上", 4, "STORY"));
                    }
                    return extracted();
                });
        stubSummaryRefresh();

        long chapterId = generateChapter(stageId);
        var oldCandidate = memoryService.listPending(storyId).get(0);

        editChapter(chapterId, "林夜检查空墙后离开。");

        assertThat(memoryService.getCandidate(oldCandidate.getId()).getProcessingStatus())
                .isEqualTo("SUPERSEDED");
        assertThat(memoryService.listPending(storyId)).isEmpty();
        mockMvc.perform(post("/api/memory/candidates/{id}/apply", oldCandidate.getId()))
                .andExpect(status().isConflict());
    }

    private long createStory(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "coreIdea", "验证当前状态来源"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private long createStage(long storyId, int planCount) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(new PlanStageResponse(
                        planCount,
                        IntStream.rangeClosed(1, planCount)
                                .mapToObj(order -> new PlanStageResponse.ChapterPlanItem(
                                        order,
                                        "目标" + order,
                                        "推进" + order,
                                        null, null, null, null, null))
                                .toList()));

        MvcResult result = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"继续当前故事\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private long generateChapter(long stageId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private void editChapter(long chapterId, String content) throws Exception {
        mockMvc.perform(put("/api/chapters/{id}/content", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk());
    }

    private void stubSummaryRefresh() {
        when(aiServiceClient.summarizeChapter(any(SummarizeChapterRequest.class)))
                .thenAnswer(invocation -> new SummarizeChapterResponse(
                        invocation.getArgument(0, SummarizeChapterRequest.class).content()));
    }

    private ExtractMemoryResponse extracted(ExtractMemoryResponse.MemoryCandidate... candidates) {
        return new ExtractMemoryResponse(List.of(candidates));
    }

    private ExtractMemoryResponse.MemoryCandidate candidate(
            String type, String subject, String field, String value) {
        return new ExtractMemoryResponse.MemoryCandidate(
                type, subject, field, value, "AUTO", value, 5, "STORY");
    }

    private void assertCurrentLocation(long storyId, String expected) {
        List<CurrentState> locations = memoryService.getCurrentState(storyId).stream()
                .filter(state -> "LOCATION".equals(state.getCategory()))
                .filter(state -> "林夜".equals(state.getSubject()))
                .filter(state -> "location".equals(state.getField()))
                .toList();
        assertThat(locations).singleElement()
                .satisfies(state -> {
                    assertThat(state.getValue()).isEqualTo(expected);
                    assertThat(state.getSourceCandidateId()).isNotNull();
                });
    }

    private void assertRelationship(long storyId, String expected) {
        List<RelationshipState> relationships = memoryService.getRelationships(storyId);
        assertThat(relationships).singleElement()
                .satisfies(relationship -> {
                    assertThat(relationship.getDescription()).isEqualTo(expected);
                    assertThat(relationship.getSourceCandidateId()).isNotNull();
                });
    }
}
