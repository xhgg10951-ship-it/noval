package com.example.storyai.arc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * TASK-152 — Arc persistence + minimal API (Phase 6).
 * Create / list / update / current-by-chapter; ranges must not overlap;
 * setting ACTIVE clears any other active arc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ArcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private long createStory() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arc 测试\",\"coreIdea\":\"长篇\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private JsonNode createArc(long storyId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        if (result.getResponse().getStatus() != 201) {
            return null;
        }
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ---- create + list + current-by-chapter ----

    @Test
    void createListAndResolveCurrentArcByChapter() throws Exception {
        long storyId = createStory();

        mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"第一卷：初入异界","goal":"立足并加入公会",
                                 "targetStartChapter":1,"targetEndChapter":60,"status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.targetStartChapter").value(1));

        mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"第二卷","goal":"远征",
                                 "targetStartChapter":61,"targetEndChapter":120}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stories/{id}/arcs", storyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("第一卷：初入异界"));

        // chapter 5 falls into arc 1's range
        mockMvc.perform(get("/api/stories/{id}/arcs/current", storyId).param("chapter", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("第一卷：初入异界"));

        // chapter 100 falls into arc 2's range
        mockMvc.perform(get("/api/stories/{id}/arcs/current", storyId).param("chapter", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("第二卷"));

        // outside every range -> 404
        mockMvc.perform(get("/api/stories/{id}/arcs/current", storyId).param("chapter", "500"))
                .andExpect(status().isNotFound());
    }

    // ---- overlap is rejected (current arc must be unambiguous) ----

    @Test
    void overlappingRangeIsRejected() throws Exception {
        long storyId = createStory();
        createArc(storyId, """
                {"title":"卷一","targetStartChapter":1,"targetEndChapter":50}
                """);

        mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"卷二重叠","targetStartChapter":40,"targetEndChapter":90}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---- invalid range rejected ----

    @Test
    void invalidRangeIsRejected() throws Exception {
        long storyId = createStory();
        mockMvc.perform(post("/api/stories/{id}/arcs", storyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"倒序卷","targetStartChapter":50,"targetEndChapter":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---- update: setting ACTIVE clears the previous active arc ----

    @Test
    void updateCanActivateArcAndClearsPreviousActive() throws Exception {
        long storyId = createStory();
        JsonNode first = createArc(storyId, """
                {"title":"卷一","targetStartChapter":1,"targetEndChapter":60,"status":"ACTIVE"}
                """);
        JsonNode second = createArc(storyId, """
                {"title":"卷二","targetStartChapter":61,"targetEndChapter":120}
                """);
        long firstId = first.get("id").asLong();
        long secondId = second.get("id").asLong();

        mockMvc.perform(get("/api/stories/{id}/arcs/current", storyId).param("chapter", "5"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/arcs/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"卷二（激活）","goal":"远征开始",
                                 "targetStartChapter":61,"targetEndChapter":120,"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // the previously active arc was demoted to PLANNED
        mockMvc.perform(get("/api/arcs/{id}", firstId))
                .andDo(r -> org.assertj.core.api.Assertions.assertThat(
                        objectMapper.readTree(r.getResponse().getContentAsString())
                                .get("status").asText()).isEqualTo("PLANNED"));
    }
}
