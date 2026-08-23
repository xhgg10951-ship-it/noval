package com.example.storyai.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal backend health endpoint.
 *
 * <p>Spring Boot Actuator is intentionally NOT a dependency (TECH_STACK keeps
 * the dependency surface minimal), so a lightweight controller exposes health.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "story-ai-backend");
        body.put("version", "0.1.1");
        return body;
    }
}
