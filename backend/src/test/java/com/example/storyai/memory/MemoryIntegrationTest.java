package com.example.storyai.memory;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.storyai.ai.dto.ExtractMemoryResponse;
import com.example.storyai.ai.dto.GenerateChapterRequest;
import com.example.storyai.ai.dto.GenerateChapterResponse;
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * M4 integration test: chapter generation triggers memory extraction; AUTO
 * candidates are auto-applied (current_state), REVIEW stays pending, IGNORE is
 * ignored; author can override a REVIEW candidate via the API
 * (TASK-027/029/032, AT-E01/E02/G01/G03/H01/H02/H03).
 *
 * <p>The Python AI Service is {@link MockitoBean @MockitoBean}ed so the test is
 * deterministic and offline. The chapter-generation flow is driven through the
 * real HTTP endpoints, which proves the TASK-032 wiring:
 * generate -> save -> extract -> save candidates -> apply AUTO -> checkpoint.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class MemoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AiServiceClient aiServiceClient;

    private Long createStory() throws Exception {
        String body = """
                {"name":"记忆测试故事","coreIdea":"末日图书馆守卫寻找最后一本书","constraints":[{"type":"TONE","content":"阴郁但带希望"}]}
                """;
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private Long createStage(Long storyId, int planCount) throws Exception {
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(new PlanStageResponse(planCount, IntStream.rangeClosed(1, planCount)
                        .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                                i, "第" + i + "章目标", "推进 " + i + "/" + planCount))
                        .toList()));
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"守卫进入禁书区\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private void stubWriterAndExtractor() {
        when(aiServiceClient.generateChapter(any(GenerateChapterRequest.class)))
                .thenAnswer(inv -> {
                    GenerateChapterRequest req = inv.getArgument(0);
                    return new GenerateChapterResponse(
                            "第" + req.chapterOrder() + "章 · 进入禁书区",
                            "主角来到禁书区。他获得了一把钥匙。他受了伤。艾琳在一旁观察，仍保持警惕。",
                            "本章主角进入禁书区并获得钥匙。");
                });
        when(aiServiceClient.extractMemory(any())).thenReturn(new ExtractMemoryResponse(java.util.List.of(
                new ExtractMemoryResponse.MemoryCandidate("CURRENT_STATE", "主角", "location", "禁书区", "AUTO", "正文提及来到禁书区。"),
                new ExtractMemoryResponse.MemoryCandidate("CURRENT_STATE", "主角", "inventory", "钥匙", "AUTO", "正文提及获得钥匙。"),
                new ExtractMemoryResponse.MemoryCandidate("RELATIONSHIP", "艾琳->主角", null, "艾琳对主角保持警惕", "REVIEW", "正文包含艾琳互动。"),
                new ExtractMemoryResponse.MemoryCandidate("STORY_MEMORY", "禁书区", null, "禁书区藏有禁忌知识", "REVIEW", "正文设定。"),
                new ExtractMemoryResponse.MemoryCandidate("STORY_MEMORY", "噪音", null, "无关噪音", "IGNORE", "无明确价值。")
        )));
    }

    @Test
    void generateChapterExtractsAndAppliesMemory() throws Exception {
        Long storyId = createStory();
        Long stageId = createStage(storyId, 1);
        stubWriterAndExtractor();

        // generate the chapter (triggers extraction + processing through TASK-032 wiring)
        Long chapterId = objectMapper.readTree(mockMvc.perform(post("/api/stages/{id}/chapters", stageId))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("id").asLong();
        assertThat(chapterId).isNotNull();

        // 5 candidates extracted and persisted
        mockMvc.perform(get("/api/stories/{id}/memory/candidates", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // AUTO candidates applied to current_state
        mockMvc.perform(get("/api/stories/{id}/memory", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").isArray())
                .andExpect(jsonPath("$.currentState[?(@.field=='location' && @.value=='禁书区')]").exists())
                .andExpect(jsonPath("$.currentState[?(@.field=='inventory' && @.value=='钥匙')]").exists())
                // REVIEW relationship not auto-applied -> relationships empty
                .andExpect(jsonPath("$.relationships.length()").value(0))
                // REVIEW story memory not auto-applied -> empty
                .andExpect(jsonPath("$.storyMemories.length()").value(0));

        // ---- author override: apply the REVIEW relationship candidate ----
        String candidatesJson = mockMvc.perform(get("/api/stories/{id}/memory/candidates", storyId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // find the RELATIONSHIP candidate id robustly
        long relationshipCandidateId = -1;
        for (var node : objectMapper.readTree(candidatesJson)) {
            if ("RELATIONSHIP".equals(node.get("type").asText())) {
                relationshipCandidateId = node.get("id").asLong();
            }
        }
        assertThat(relationshipCandidateId).isNotEqualTo(-1);

        mockMvc.perform(post("/api/memory/candidates/{id}/apply", relationshipCandidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("APPLIED"))
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(get("/api/stories/{id}/memory", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationships.length()").value(1))
                .andExpect(jsonPath("$.relationships[0].description").value(org.hamcrest.Matchers.containsString("警惕")));

        // ---- author ignore override of an AUTO candidate ----
        long autoLocationId = -1;
        for (var node : objectMapper.readTree(candidatesJson)) {
            if ("CURRENT_STATE".equals(node.get("type").asText()) && "location".equals(node.get("field").asText())) {
                autoLocationId = node.get("id").asLong();
            }
        }
        mockMvc.perform(post("/api/memory/candidates/{id}/ignore", autoLocationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("IGNORED"));
    }
}
