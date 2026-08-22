package com.example.storyai.stage;

import java.util.List;

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
import com.example.storyai.ai.dto.PlanStageRequest;
import com.example.storyai.ai.dto.PlanStageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Stage Planning vertical slice (TASK-011..015).
 *
 * <p>The Python AI Service is {@link MockitoBean @MockitoBean}ed so the tests
 * run offline; the planner contract (request shape in, structured response
 * out) is exactly what the real service implements.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StagePlanningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    // ---- helpers ----

    private Long createStory() throws Exception {
        String body = """
                {"name":"规划测试故事","coreIdea":"主角在异世界逐步成长","constraints":[{"type":"风格","content":"冷峻克制"}]}
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
        return new PlanStageResponse(count, java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlanStageResponse.ChapterPlanItem(
                        i, goalPrefix + "：第" + i + "章目标", "推进 " + i + "/" + count,
                        null, null, null, null, null))
                .toList());
    }

    // ---- AT-B01: create stage with suggested chapter count + plan ----

    @Test
    void createStageReturnsPlannedChapters() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "初版"));

        mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"主角和艾琳前往冒险者公会完成注册\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.suggestedChapterCount").value(3))
                .andExpect(jsonPath("$.plans.length()").value(3))
                .andExpect(jsonPath("$.plans[0].chapterOrder").value(1))
                .andExpect(jsonPath("$.plans[2].chapterOrder").value(3));

        // Planner request must carry the story context.
        org.mockito.ArgumentCaptor<PlanStageRequest> captor =
                org.mockito.ArgumentCaptor.forClass(PlanStageRequest.class);
        org.mockito.Mockito.verify(aiServiceClient).planStage(captor.capture());
        PlanStageRequest sent = captor.getValue();
        assertThat(sent.coreIdea()).isEqualTo("主角在异世界逐步成长");
        assertThat(sent.stageDirection()).isEqualTo("主角和艾琳前往冒险者公会完成注册");
        assertThat(sent.constraints()).hasSize(1);
    }

    // ---- AT-B02: replan regenerates the full plan (not truncation) ----

    @Test
    void replanReplacesPlanWithRegeneratedGoals() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(5, "初版"));
        when(aiServiceClient.replanStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(2, "重规划"));

        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"完成公会注册并初次展示力量\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(post("/api/stages/{id}/replan", stageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetChapterCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedChapterCount").value(2))
                .andExpect(jsonPath("$.targetChapterCount").value(2))
                .andExpect(jsonPath("$.plans.length()").value(2))
                .andExpect(jsonPath("$.plans[0].goal").value("重规划：第1章目标"))
                .andExpect(jsonPath("$.plans[1].goal").value("重规划：第2章目标"));

        // Old 5-plan rows were replaced, not truncated: exactly 2 rows remain.
        var replanCaptor = org.mockito.ArgumentCaptor.forClass(PlanStageRequest.class);
        org.mockito.Mockito.verify(aiServiceClient).replanStage(replanCaptor.capture());
        assertThat(replanCaptor.getValue().targetChapterCount()).isEqualTo(2);

        mockMvc.perform(get("/api/stages/{id}", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(2));
    }

    // ---- AT-B03: edit chapter goal, then confirm the plan ----

    @Test
    void editGoalThenConfirmActivatesStage() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(planOf(3, "初版"));
        MvcResult created = mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"测试方向\"}"))
                .andReturn();
        long stageId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult stage = mockMvc.perform(get("/api/stages/{id}", stageId))
                .andExpect(status().isOk())
                .andReturn();
        long planId = objectMapper.readTree(stage.getResponse().getContentAsString())
                .get("plans").get(1).get("id").asLong();

        mockMvc.perform(put("/api/stages/plans/{id}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"不允许主角在这一章暴露真实修仙身份\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("不允许主角在这一章暴露真实修仙身份"));

        mockMvc.perform(post("/api/stages/{id}/confirm", stageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---- Contract violation -> 502 AI_SERVICE_ERROR ----

    @Test
    void invalidPlannerResponseYields502() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.planStage(any(PlanStageRequest.class)))
                .thenReturn(new PlanStageResponse(3, List.of())); // count != plans

        mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"方向\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_ERROR"));
    }

    // ---- validation: blank direction -> 400 ----

    @Test
    void blankDirectionRejected() throws Exception {
        Long storyId = createStory();
        mockMvc.perform(post("/api/stories/{id}/stages", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
