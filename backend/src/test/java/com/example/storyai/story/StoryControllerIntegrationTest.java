package com.example.storyai.story;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end REST integration test for the Story API (TASK-009, AT-A01).
 * DB-gated + rollback so it never pollutes the dev database:
 * <pre>DB_USERNAME=story_dev DB_PASSWORD=storypass mvn test</pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGetListStory() throws Exception {
        String body = """
                {"name":"集成测试故事","coreIdea":"穿越者异世界求生",
                 "initialStageDirection":"主角初到风息镇",
                 "constraints":[
                   {"type":"STYLE","content":"轻松爽文式风格"},
                   {"type":"PERSPECTIVE","content":"第三人称限知"}
                 ]}
                """;

        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("集成测试故事"))
                .andExpect(jsonPath("$.constraints", hasSize(2)))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/stories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coreIdea").value("穿越者异世界求生"))
                .andExpect(jsonPath("$.initialStageDirection").value("主角初到风息镇"));

        mockMvc.perform(get("/api/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id.intValue())));
    }

    @Test
    void createInvalidReturns400() throws Exception {
        mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"coreIdea\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.coreIdea").exists());
    }

    @Test
    void getMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/stories/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
