package com.example.storyai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.storyai.common.HealthController;

/**
 * Web-layer test for the backend health endpoint.
 *
 * <p>Uses {@link WebMvcTest} so the full application context (and therefore the
 * MySQL DataSource) is NOT required — this keeps `mvn test` DB-independent for
 * the M0 bootstrap. A live `spring-boot:run` smoke test separately proves the
 * app starts end to end.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("story-ai-backend"));
    }
}
