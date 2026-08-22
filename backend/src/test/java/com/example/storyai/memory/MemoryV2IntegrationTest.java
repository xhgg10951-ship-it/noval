package com.example.storyai.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

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
import com.example.storyai.chapter.model.MemoryExtractionStatus;
import com.example.storyai.memory.model.MemoryCandidate;
import com.example.storyai.memory.service.CandidateProcessingService;
import com.example.storyai.memory.service.MemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Phase 7 — Memory v2 integration tests.
 *
 * <pre>
 * TASK-158/161: unknown type -> REVIEW, never auto-stored
 * TASK-159:    importance clamped, scope normalized
 * TASK-162:    dedup — same fact not inserted twice (AC-115)
 * TASK-163:    inventory multi-item slots coexist (AC-116)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class MemoryV2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateProcessingService processingService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private com.example.storyai.context.StoryContextReader contextReader;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    private long createStory() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MemoryV2\",\"coreIdea\":\"测试\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private MemoryCandidate candidate(long storyId, String type, String subject,
                                      String field, String value, String action) {
        MemoryCandidate c = new MemoryCandidate();
        c.setStoryId(storyId);
        c.setSourceChapterId(null);
        c.setType(type);
        c.setSubject(subject);
        c.setField(field);
        c.setValue(value);
        c.setSuggestedAction(action);
        c.setEvidence("evidence");
        return c;
    }

    // ---- TASK-158/161: unknown type routes to REVIEW, never stored as fact ----

    @Test
    void unknownTypeRoutesToReviewAndNeverApplied() throws Exception {
        long storyId = createStory();
        MemoryCandidate weird = candidate(storyId, "MYSTICAL_VIBES", "林夜", null,
                "身上有神秘气息", "AUTO");

        processingService.autoProcess(weird);

        assertThat(weird.getProcessingStatus()).isEqualTo("PENDING");
        assertThat(weird.isApplied()).isFalse();
        // no STORY_MEMORY row was created for the unknown type
        assertThat(memoryService.findActiveByTypeSubject(storyId, "MYSTICAL_VIBES", "林夜"))
                .isEmpty();
    }

    // ---- TASK-159: importance clamped 1..5, scope normalized ----

    @Test
    void importanceClampedAndScopeNormalized() throws Exception {
        long storyId = createStory();

        MemoryCandidate high = candidate(storyId, "PLOT_FACT", "林夜", null,
                "获得了一枚古旧徽章", "AUTO");
        high.setImportance(99);
        high.setScope("galaxy");
        processingService.autoProcess(high);

        List<com.example.storyai.memory.model.StoryMemory> stored =
                memoryService.findActiveByTypeSubject(storyId, "PLOT_FACT", "林夜");
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getImportance()).isEqualTo(5); // clamped from 99
        assertThat(stored.get(0).getScope()).isEqualTo("STORY"); // normalized from "galaxy"
    }

    // ---- TASK-162 / AC-115: exact-fact dedup ----

    @Test
    void duplicateFactIsNotStoredTwice() throws Exception {
        long storyId = createStory();

        MemoryCandidate first = candidate(storyId, "PLOT_FACT", "艾琳", null,
                "与林夜约定在公会再次见面", "AUTO");
        processingService.autoProcess(first);

        MemoryCandidate dup = candidate(storyId, "PLOT_FACT", "艾琳", null,
                "与林夜约定在公会再次见面", "AUTO");
        dup.setSourceChapterId(999L); // different chapter, same normalized fact
        processingService.autoProcess(dup);

        List<com.example.storyai.memory.model.StoryMemory> stored =
                memoryService.findActiveByTypeSubject(storyId, "PLOT_FACT", "艾琳");
        assertThat(stored).hasSize(1); // AC-115: only ONE row for the same fact

        // a DIFFERENT fact still lands normally
        MemoryCandidate other = candidate(storyId, "PLOT_FACT", "艾琳", null,
                "透露自己曾是王国骑士团成员", "AUTO");
        processingService.autoProcess(other);
        assertThat(memoryService.findActiveByTypeSubject(storyId, "PLOT_FACT", "艾琳"))
                .hasSize(2);
    }

    // ---- TASK-163 / AC-116: inventory multi-item slots coexist ----

    @Test
    void inventorySlotsPerItemCoexistAndDeleteIndependently() throws Exception {
        long storyId = createStory();

        processingService.applyCandidate(candidate(
                storyId, "CURRENT_STATE", "林夜", "inventory", "铁剑", "AUTO"));
        processingService.applyCandidate(candidate(
                storyId, "CURRENT_STATE", "林夜", "item", "治疗药水", "AUTO"));

        var state = memoryService.getCurrentState(storyId);
        List<String> fields = state.stream()
                .filter(s -> "INVENTORY".equals(s.getCategory()))
                .map(com.example.storyai.memory.model.CurrentState::getField)
                .toList();
        // AC-116: both items exist simultaneously as separate slots
        assertThat(fields).containsExactlyInAnyOrder("item:铁剑", "item:治疗药水");

        // losing one item removes ONLY its slot (reverse lookup by slot key)
        int removed = memoryService.deleteCurrentStateSlot(storyId, "INVENTORY", "林夜", "item:铁剑");
        assertThat(removed).isEqualTo(1);
        fields = memoryService.getCurrentState(storyId).stream()
                .filter(s -> "INVENTORY".equals(s.getCategory()))
                .map(com.example.storyai.memory.model.CurrentState::getField)
                .toList();
        assertThat(fields).containsExactly("item:治疗药水");
    }

    // ---- TASK-164: writer memory selection (bread-loop prevention plumbing) ----

    @Test
    void writerSelectionExcludesTransientAndLowImportanceMemories() throws Exception {
        long storyId = createStory();

        // seed: core fact, active thread, and the notorious bread detail
        seedAndApply(storyId, "PLOT_FACT", "林夜", null, "已注册为初级冒险者", 5, "STORY");
        seedAndApply(storyId, "PLOT_THREAD", "神秘商人", null, "许诺三天后交付一件关键道具", 4, "ARC");
        seedAndApply(storyId, "TRANSIENT_DETAIL", "林夜", null, "早餐买了一个普通面包", 1, "CHAPTER");
        seedAndApply(storyId, "WORLD_RULE", "大陆", null, "魔力潮汐每百年一次", 2, "STORY");

        var items = contextReader.getWriterMemoryItems(storyId);
        List<String> descriptions = items.stream()
                .map(com.example.storyai.ai.dto.GenerateChapterRequest.MemoryItem::description)
                .toList();

        assertThat(descriptions).contains("已注册为初级冒险者");           // importance 5 kept
        assertThat(descriptions).contains("许诺三天后交付一件关键道具");   // thread kept
        assertThat(descriptions).doesNotContain("早餐买了一个普通面包");   // transient excluded
        assertThat(descriptions).doesNotContain("魔力潮汐每百年一次");     // importance<=2 excluded

        long breadMentions = countBreadInWriterRequest(storyId);
        assertThat(breadMentions).isZero(); // the writer literally never sees "bread"
    }

    private long countBreadInWriterRequest(long storyId) {
        var items = contextReader.getWriterMemoryItems(storyId);
        var states = contextReader.getWriterStateItems(storyId);
        long bread = items.stream().filter(i -> i.description() != null && i.description().contains("面包")).count();
        long breadState = states.stream().filter(s -> s.value() != null && s.value().contains("面包")).count();
        return bread + breadState;
    }

    private void seedAndApply(long storyId, String type, String subject,
                              String field, String value, int importance, String scope) {
        MemoryCandidate c = candidate(storyId, type, subject, field, value, "AUTO");
        c.setImportance(importance);
        c.setScope(scope);
        processingService.autoProcess(c);
    }
}

