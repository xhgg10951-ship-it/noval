package com.example.storyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Story Co-Author v0.1.1 — Spring Boot backend entrypoint.
 *
 * <p>Architecture principle: "AI proposes. Java decides. MySQL remembers."
 * Vue must never call the Python AI Service directly; it always goes through
 * here so business state stays authoritative.
 */
@SpringBootApplication
public class StoryAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoryAiApplication.class, args);
    }
}
