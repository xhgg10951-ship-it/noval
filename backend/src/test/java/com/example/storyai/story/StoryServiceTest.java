package com.example.storyai.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.common.exception.ResourceNotFoundException;
import com.example.storyai.story.model.Story;
import com.example.storyai.story.model.StoryConstraint;
import com.example.storyai.story.service.StoryService;

/**
 * Integration test for Story persistence (TASK-008). Needs a live MySQL, so it
 * only runs when DB_PASSWORD is set:
 * <pre>DB_USERNAME=story_dev DB_PASSWORD=storypass mvn test</pre>
 *
 * <p>Class-level {@link Transactional} rolls back every test's writes so the dev
 * database stays clean. Each test is self-contained (no cross-test ordering).
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StoryServiceTest {

    @Autowired
    private StoryService storyService;

    @Test
    void createStoryPersistsStoryAndConstraints() {
        Story story = new Story();
        story.setName("AT 测试故事");
        story.setCoreIdea("穿越者异世界求生，逐步揭示玉佩秘密");
        story.setInitialStageDirection("主角初到风息镇");

        StoryConstraint style = new StoryConstraint();
        style.setType("STYLE");
        style.setContent("轻松爽文式风格");

        StoryConstraint perspective = new StoryConstraint();
        perspective.setType("PERSPECTIVE");
        perspective.setContent("第三人称限知");

        Story created = storyService.createStory(story, List.of(style, perspective));
        assertNotNull(created.getId(), "generated id should be populated");

        Story loaded = storyService.getStory(created.getId());
        assertEquals("AT 测试故事", loaded.getName());
        assertEquals("穿越者异世界求生，逐步揭示玉佩秘密", loaded.getCoreIdea());
        assertEquals("ACTIVE", loaded.getStatus());

        List<StoryConstraint> constraints = storyService.getConstraints(created.getId());
        assertEquals(2, constraints.size());
        assertEquals("STYLE", constraints.get(0).getType());
        assertEquals("第三人称限知", constraints.get(1).getContent());
    }

    @Test
    void getStoryMissingThrows() {
        long missingId = 9_999_999L;
        try {
            storyService.getStory(missingId);
        } catch (ResourceNotFoundException expected) {
            return;
        }
        throw new AssertionError("expected ResourceNotFoundException for missing story");
    }

    @Test
    void listStoriesIncludesInsertedRow() {
        Story story = new Story();
        story.setName("列表测试故事");
        story.setCoreIdea("用于列表测试");
        storyService.createStory(story, List.of());

        List<Story> all = storyService.listStories();
        assertNotNull(all);
        assertTrue(
                all.stream().anyMatch(s -> story.getId().equals(s.getId())),
                "list should include the just-inserted story");
    }
}
