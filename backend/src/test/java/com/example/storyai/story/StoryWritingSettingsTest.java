package com.example.storyai.story;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TASK-150 — Story targetChapterCount CRUD basics (Phase 6).
 *
 * <pre>
 * create with targetChapterCount -> persisted and echoed
 * PATCH /writing-settings partial semantics: null = leave unchanged
 * legacy stories (created without it) stay NULL until the author sets one
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StoryWritingSettingsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private long createStory(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    // ---- create carries targetChapterCount ----

    @Test
    void createWithTargetChapterCountPersistsAndEchoes() throws Exception {
        long id = createStory("""
                {"name":"节奏测试","coreIdea":"长篇异世界成长","targetChapterCount":600,
                 "writingStyle":"冷峻克制"}
                """);

        mockMvc.perform(patch("/api/stories/{id}/writing-settings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetChapterCount").value(600))
                .andExpect(jsonPath("$.writingStyle").value("冷峻克制"));
    }

    // ---- legacy story: NULL allowed, PATCH sets it ----

    @Test
    void legacyStoryIsNullUntilAuthorSetsTarget() throws Exception {
        long id = createStory("""
                {"name":"无目标旧故事","coreIdea":"普通短篇"}
                """);

        mockMvc.perform(patch("/api/stories/{id}/writing-settings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetChapterCount").doesNotExist());

        mockMvc.perform(patch("/api/stories/{id}/writing-settings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetChapterCount\":300,\"defaultTargetCharacters\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetChapterCount").value(300))
                .andExpect(jsonPath("$.defaultTargetCharacters").value(2500));
    }

    // ---- partial update: omitted fields keep their values; out-of-range rejected ----

    @Test
    void partialPatchKeepsUnspecifiedFields() throws Exception {
        long id = createStory("""
                {"name":"部分更新","coreIdea":"x","targetChapterCount":100,"defaultTargetCharacters":2000}
                """);

        mockMvc.perform(patch("/api/stories/{id}/writing-settings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultTargetCharacters\":4000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultTargetCharacters").value(4000))
                .andExpect(jsonPath("$.targetChapterCount").value(100));

        mockMvc.perform(patch("/api/stories/{id}/writing-settings", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetChapterCount\":9999}"))
                .andExpect(status().isBadRequest());
    }
}
