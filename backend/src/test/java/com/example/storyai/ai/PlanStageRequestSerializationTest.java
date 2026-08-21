package com.example.storyai.ai;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.storyai.ai.dto.PlanStageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Diagnoses TASK-015 live failure: confirms the Spring-managed ObjectMapper
 * serializes a PlanStageRequest to a non-empty JSON body (the live RestClient
 * was sending an EMPTY body -> Python 422). Documents the serialization contract.
 */
@SpringBootTest
class PlanStageRequestSerializationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void springObjectMapperSerializesRecordToNonEmptyJson() throws Exception {
        PlanStageRequest req = new PlanStageRequest(
                "core idea",
                List.of(new PlanStageRequest.ConstraintItem("type", "content")),
                "stage direction",
                List.of(),
                List.of(),
                "",
                3);
        String json = objectMapper.writeValueAsString(req);
        System.out.println("SPRING_OM_JSON=" + json);
        System.out.println("SPRING_OM_LEN=" + json.length());
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("coreIdea"));
        org.junit.jupiter.api.Assertions.assertTrue(json.length() > 10);
    }
}
