# v0.1 Regression Evidence Snapshot

> **Purpose**: Phase 0 — Correct Evidence Base (TASK-101).
>
> This document freezes the *real* v0.1 behavior as a regression baseline
> **before** any v0.1.1 code change. It is evidence, not a spec.
> It must NOT be overwritten by later implementation; later phases append
> their own acceptance evidence instead.
>
> Captured on branch `v0.1.1-dev` at commit `fd8ace0`
> (`docs: sync frozen v0.1.1 specifications`). Working tree clean.
>
> Status of v0.1: **Engineering Pipeline Accepted** only.
> **Real-LLM Product Behavior: NOT ACCEPTED** (no real-LLM semantic run on record).

---

## 1. Environment / Model Configuration

Source: `ai-service/app/config.py`

- LLM backend selected by env vars. Accepted names (first non-empty wins):
  - key: `LLM_API_KEY` | `API_KEY`
  - base_url: `LLM_BASE_URL` | `API_URL`
  - model: `LLM_MODEL` | `MODEL` | `model`
- Default model when unset: **`qwen3-8b`**
- `settings.using_mock_llm` is `True` when no API key is present.
- `provider.get_provider()` returns `LangChainProvider` when a key exists,
  else `MockProvider`. (Provider routing was repaired in the v0.1.1-dev
  baseline commit `d39c33f`; this is the current baseline.)
- LangChain provider uses an OpenAI-compatible Chat endpoint and disables
  qwen `thinking` mode for speed.

> **Evidence note**: the earlier v0.1 sessions ran Mock by default because the
> configured vars were `API_KEY`/`API_URL`, which the *old* provider did not
> read. That routing bug is already fixed in the baseline; the behavioral
> gaps below are independent of it.

---

## 2. AI Request Contracts (what Java is allowed to send)

These DTOs already *exist* with the right fields — the gap is that Java
sends them empty (see §3).

### `PlanStageRequest` (Planner)
Fields present: `coreIdea`, `constraints[]`, `stageDirection`,
`currentState[]`, `storyMemories[]`, `recentContext`, `targetChapterCount`.

### `GenerateChapterRequest` (Writer)
Fields present: `coreIdea`, `constraints[]`, `stageDirection`,
`chapterGoal`, `chapterOrder`, `currentState[]`, `storyMemories[]`,
`relationshipState[]`, `recentContext`.

> **Contract gap**: neither request carries `targetCharacters`,
> `mustAdvance`, `mustNotDo`, `storyBeats`, `endingIntent`,
> `continuationAnchor`, `currentChapterNumber`, `relationshipState` (Planner),
> or `chapterSpec`. The frozen plan adds these in Phase 1–2.

---

## 3. Java Context-Assembly Reality (root-cause proof)

### RC-01 — Planner receives empty existing-story context
File: `backend/src/main/java/com/example/storyai/stage/service/StagePlanningService.java`
Method: `buildRequest(...)` (lines 87–103)

```java
return new PlanStageRequest(
        story.getCoreIdea(),
        constraintItems,
        direction,
        List.of(),   // currentState  -> EMPTY
        List.of(),   // storyMemories -> EMPTY
        "",          // recentContext -> EMPTY
        targetChapterCount
);
```

Consequence: a new Stage is planned with **no knowledge of prior plot**,
so the Planner tends to behave like a fresh-book opening. This is the direct
cause of the "every Stage restarts the story" symptom.

### RC-02 — Writer receives empty state / memory
File: `backend/src/main/java/com/example/storyai/chapter/service/ChapterGenerationService.java`
Method: `buildRequest(...)` (lines 124–143)

```java
return new GenerateChapterRequest(
        story.getCoreIdea(),
        constraintItems,
        stage.getDirection(),
        plan.getGoal(),
        plan.getChapterOrder(),
        List.of(),   // currentState       -> EMPTY
        List.of(),   // storyMemories     -> EMPTY
        List.of(),   // relationshipState -> EMPTY
        prevSummary == null ? "" : prevSummary   // only prev chapter summary
);
```

Consequence: Structured Memory / Current State / Relationships are **not**
wired into generation. The only continuity input is the single previous
chapter summary (`prevSummary`, lines 86–89).

### RC-04 — Single-summary feedback loop (confirmed in code)
`ChapterGenerationService.generateNextChapter` uses, as recent context,
**only** the most recent chapter's `summary` (lines 86–89). There is no
window of 2–3 summaries, no last-ending excerpt, no continuation anchor.
This matches the observed loop:
`low-value detail → summary repeats it → next chapter gets only that summary → repeats`.

### RC-05 — ChapterPlan progress underused
The DB `ChapterPlan` holds `goal` + `expectedProgress`, but `buildRequest`
sends only `plan.getGoal()`. `expectedProgress` is dropped before reaching
the Writer.

---

## 4. Python Prompt Text (verbatim, current baseline)

### Planner system prompt (`build_plan_prompt`)
```
你是一名小说分章策划 AI。根据作者的'阶段导演指令'、故事约束、当前状态与记忆，
输出一个分阶段章节计划。必须只返回严格 JSON，格式为：
{"suggestedChapterCount": int, "chapterPlans": [{"order": int, "goal": str, "expectedProgress": str}]}
```

Gaps:
- No "You are continuing an existing story" instruction.
- No current chapter number, no Arc, no continuation anchor.
- Receives `currentState`/`storyMemories`/`recentContext` *slots* but Java
  sends them empty (see §3).

### Writer system prompt (`build_generate_prompt`)
```
你是一名小说章节写作 AI。根据阶段导演指令、本章目标与上下文，写出一章连贯的叙事正文，
并附标题与摘要。必须只返回严格 JSON，格式为：
{"title": str, "content": str, "summary": str}
```

Gaps:
- **No target length / `targetCharacters`** → no length contract (RC-06).
- Only `chapterGoal` from the plan; `expectedProgress` not forwarded (RC-05).
- No `mustAdvance` / `mustNotDo` / `storyBeats` / `endingIntent`.
- No priority ordering (Hard Constraints > Long-form > Arc > Stage >
  ChapterSpec > State > Memory > Recent). Memory is a flat list with no
  selection / exclusion of low-value items.

### Memory Extractor system prompt (`build_extract_prompt`)
```
你是一名故事记忆抽取 AI。从章节正文与摘要中，抽取应当被长期记住的事实：
当前状态(CURRENT_STATE)、人物关系(RELATIONSHIP)、故事记忆(STORY_MEMORY，含伏笔)。
每个候选给出 subject、可选 field、value、suggestedAction(AUTO 自动采纳 / REVIEW 需作者确认 / IGNORE 忽略)、
以及 evidence 证据原文。必须只返回严格 JSON...
```

Gaps:
- Only three types: `CURRENT_STATE`, `RELATIONSHIP`, `STORY_MEMORY`.
  No `PLOT_FACT` / `PLOT_THREAD` / `FORESHADOWING` / `WORLD_RULE` /
  `TRANSIENT_DETAIL` (RC-13).
- No `importance`, no `scope` (RC-14).
- No dedup key (RC-14).

---

## 5. Mock Provider Baseline Behavior (concrete repetitive artifact)

Source: `ai-service/app/services/mock_builders.py`

The Mock Writer emits the **same fixed details every chapter**:

```
他获得了一把生锈的铜钥匙，握在掌心仍有余温。
玉佩贴在胸口，微微发烫，只是他尚未明白其中缘由。
艾琳在一旁观察着他，神色间仍有戒备，却也难免流露出几分关切。
（本章为 Mock Provider 生成的占位正文，用于在无 LLM_API_KEY 时验证端到端链路。）
```

The Mock Extractor reinforces them deterministically:
- `CURRENT_STATE` 主角 location (regex on 来到/前往/... )
- `CURRENT_STATE` 主角 inventory = "生锈的铜钥匙" (regex on 获得/得到/...)
- `CURRENT_STATE` 主角 physical_condition = "受伤"
- `RELATIONSHIP` 艾琳->主角 (always, `REVIEW`)
- `STORY_MEMORY` 玉佩/黑袍 伏笔 (always, `REVIEW`)

> **Why this matters**: this is the *mechanical* analogue of the "bread loop".
> Because Mock output is fixed, Mock can never prove writing quality,
> continuation, length control, or low-value-detail isolation. It only proves
> the HTTP/DTO/persistence/parsing plumbing. (AC-117 boundary.)

---

## 6. Known Regression Cases (to be retested in later phases)

| Case | Symptom | Root cause (proven in code) | Target acceptance |
|------|---------|------------------------------|-------------------|
| Too-short chapters | ~500 chars/chapter | No `targetCharacters` anywhere (RC-06) | AC-103: target 3000, ±25% |
| Restart-the-book | New Stage re-opens story (穿越/初遇/找住处) | `StagePlanningService` sends empty `currentState`/`storyMemories` (RC-01) | AC-101 |
| Bread / detail loop | A trivial detail becomes a recurring anchor | Only last summary used as continuity (RC-04); Memory not weighted/excluded | AC-105 |
| Plan ignored | Writer drifts from ChapterPlan | Only `goal` forwarded; no `expectedProgress`/spec (RC-05) | AC-104 |
| Insecure mid-stage replan | Full `replacePlans()` deletes history | `StageService.replacePlans` (RC-08) | AC-106 |
| Uneditable chapters | No edit/regenerate/polish | Chapter API is generate/read only (RC-07) | AC-107/108/109 |
| No long-form pace | Shortest path to "finish" | Planner unaware of book length / Arc (RC-09) | AC-114 |

---

## 7. Verification Status (as of snapshot)

| Layer | Status |
|-------|--------|
| Backend build / unit tests | historically passing (v0.1 freeze) |
| Python tests (mock path) | historically passing |
| Frontend build | historically passing |
| Real-LLM semantic run | **NONE ON RECORD** — v0.1 is Engineering-Accepted only |

> Per the frozen rule: **Mock PASS ≠ Semantic PASS.** Every AI-behavior
> acceptance (AC-101..AC-117) must be re-run against the configured real LLM
> (qwen3-8b) during Phase 9 / per-phase semantic gates.

---

## 8. Limitations of THIS snapshot

- A **live MySQL runtime dump** (actual Story / Stage / Chapter / Memory rows)
  was NOT captured in this session because the backend + MySQL services were
  not running. The code-level evidence above is authoritative for the
  *behavior* baseline. A live data snapshot should be re-taken once Phase 1
  wiring makes real-LLM generation observable, and appended here.
- Prompts quoted verbatim from `ai-service/app/prompts/builders.py` at the
  baseline commit. If a later commit changes them, re-capture and add a new
  dated section rather than editing this one.

---

## 9. Recovery Hook

If a new Agent resumes: this file + `V0.1.1_IMPROVEMENT_PLAN.md` (root
causes RC-01..RC-16) + the code facts in §3 are the regression baseline.
Phase 1 begins by *wiring* the already-present DTO fields (no new fields
yet); Phase 2 adds the ChapterSpec / length fields.
