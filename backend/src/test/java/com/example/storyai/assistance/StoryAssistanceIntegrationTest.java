package com.example.storyai.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.storyai.ai.dto.StoryQueryRequest;
import com.example.storyai.ai.dto.StoryQueryResponse;
import com.example.storyai.ai.dto.SuggestDirectionsRequest;
import com.example.storyai.ai.dto.SuggestDirectionsResponse;
import com.example.storyai.ai.dto.SuggestDirectionsResponse.DirectionItem;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for author assistance (M6 / TASK-042..046).
 * Covers AT-K01 (>=3 distinct directions), AT-K03 (read-only, no story change),
 * AT-J01..J05 (story query answers from structured memory). AI Service mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StoryAssistanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private Long createStory() throws Exception {
        String body = """
                {"name":"协助测试故事","coreIdea":"天帝穿越到西幻世界","constraints":[{"type":"视角","content":"第三人称"}]}
                """;
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ---- AT-K01: at least 3 distinct directions returned ----

    @Test
    void suggestDirectionsReturnsThreeDistinct() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.suggestDirections(any(SuggestDirectionsRequest.class)))
                .thenReturn(new SuggestDirectionsResponse(java.util.List.of(
                        new DirectionItem("冲突型", "暴露部分力量引发冲突"),
                        new DirectionItem("成长型", "了解规则建立信任"),
                        new DirectionItem("悬疑型", "围绕玉佩埋下伏笔")
                )));

        mockMvc.perform(post("/api/stories/{id}/suggest-directions", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directions.length()").value(3))
                .andExpect(jsonPath("$.directions[0].title").value("冲突型"))
                .andExpect(jsonPath("$.directions[2].title").value("悬疑型"));

        // AT-K03: the suggestion call must not create stages/chapters — read-only.
        verify(aiServiceClient).suggestDirections(any(SuggestDirectionsRequest.class));
    }

    // ---- AT-K03: suggestions do not auto-modify the story ----

    @Test
    void suggestDirectionsIsReadOnly() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.suggestDirections(any(SuggestDirectionsRequest.class)))
                .thenReturn(new SuggestDirectionsResponse(java.util.List.of(
                        new DirectionItem("A", "x"), new DirectionItem("B", "y"), new DirectionItem("C", "z"))));

        mockMvc.perform(post("/api/stories/{id}/suggest-directions", storyId)).andExpect(status().isOk());

        // Story still has no stages after a suggestion call.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/stories/{id}/stages", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- AT-J01..J05: story query answers from assembled context ----

    @Test
    void storyQueryAnswersFromStructuredMemory() throws Exception {
        Long storyId = createStory();
        when(aiServiceClient.storyQuery(any(StoryQueryRequest.class)))
                .thenAnswer(inv -> {
                    StoryQueryRequest req = inv.getArgument(0);
                    String q = req.question();
                    // mirror the mock_query behavior the real service uses
                    if (q.contains("位置") || q.contains("在哪里")) {
                        return new StoryQueryResponse("当前位置：冒险者公会。");
                    }
                    if (q.contains("物品") || q.contains("持有")) {
                        return new StoryQueryResponse("当前持有：玉佩。");
                    }
                    if (q.contains("关系") || q.contains("艾琳")) {
                        return new StoryQueryResponse("关系状态：艾琳对主角保持警惕但有所降低。");
                    }
                    if (q.contains("伏笔")) {
                        return new StoryQueryResponse("已知伏笔：玉佩发热。");
                    }
                    return new StoryQueryResponse("当前故事中没有确定这一信息（未知）。");
                });

        // location
        mockMvc.perform(post("/api/stories/{id}/story-query", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"主角现在在哪里？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("当前位置：冒险者公会。"));

        // unknown
        mockMvc.perform(post("/api/stories/{id}/story-query", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"主角的父亲叫什么？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("当前故事中没有确定这一信息（未知）。"));

        // The assembled request carried the story's constraints + structured memory.
        var captor = org.mockito.ArgumentCaptor.forClass(StoryQueryRequest.class);
        verify(aiServiceClient, org.mockito.Mockito.atLeastOnce()).storyQuery(captor.capture());
        var lastReq = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastReq.currentState()).isNotNull();
        assertThat(lastReq.relationshipState()).isNotNull();
        assertThat(lastReq.storyMemories()).isNotNull();
    }
}
