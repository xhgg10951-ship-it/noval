package com.example.storyai.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes backend↔AI Service status so the Java/Python boundary can be probed
 * without business logic (TASK-006).
 */
@RestController
@RequestMapping("/api/ai")
public class AiServiceController {

    private final AiServiceClient client;

    public AiServiceController(AiServiceClient client) {
        this.client = client;
    }

    @GetMapping("/health")
    public AiHealthResponse aiHealth() {
        return client.checkHealth();
    }
}
