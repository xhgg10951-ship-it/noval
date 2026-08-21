# STATE — AI Story Co-Author v0.1

_Updated: 2026-08-21 (M3 complete — Single Chapter Generation vertical slice live-verified)_

## Current Project State
- **Current Milestone:** `M3 — Single Chapter Generation` → **COMPLETE**
- **Current Task:** M3 gate closed; all of TASK-001..TASK-024 → `DONE`
- **Next Task:** `M4 — Memory Vertical Slice` → start at `TASK-025 — Design Minimum Memory Schema`

## M2 Verified Deliverables (all committed)
- **TASK-011** Stage + ChapterPlan min schema: `stage` (status PLANNING/ACTIVE/COMPLETED/ABANDONED, suggested+target chapter counts) + `chapter_plan` (order, goal, expected_progress), idempotent V2 migration — `fb358df`
- **TASK-012** Stage persistence: StageMapper/ChapterPlanMapper (interface + XML; insert/find/updateStatus/updatePlanCounts, batch insert, deleteByStageId) + StageService (`@Transactional` saveNewStage / replacePlans full replan / confirmPlan / updatePlanGoal) — `fb358df`
- **TASK-013** Java↔Python planner contract: `PlanStageRequest`/`PlanStageResponse` records matching Pydantic (`coreIdea`/`stageDirection`/`constraints`/`currentState`/`storyMemories`/`recentContext`/`targetChapterCount`) — `fb358df`
- **TASK-014** Python planner (mock-first, deterministic `mock_plan`); pytest 7/7 — `fb358df`
- **TASK-015** StagePlanningService: load story → build request → call Python OUTSIDE tx → validate structured response (count consistency, contiguous orders, non-blank goals) → transactional save; `AiServiceException` → 502, `ResourceAccessException` → 502 — `fb358df`
- **TASK-016** Replan: full regenerate (not truncate) for a new target count — `fb358df`
- **TASK-017** Planning UI: `frontend/src/api/stages.ts` (typed client w/ correct paths `/stories/{id}/stages`, `/stages/{id}`, `/stages/{id}/replan`, `/stages/{id}/confirm`, `/stages/plans/{id}`), `StagePlanning.vue` (direction input → plan display → edit goal → replan → confirm), wired into `StoryDetailView` — `this commit`
- **M2 gate (AT-B01/B02/B03)**: live-verified via Vite proxy (`:5173/api`): AT-B01 create stage plan (201, 3 chapters from mock planner, persisted to MySQL); AT-B02 replan → target 2 chapters (200, full regen to 2 chapters `完成第 1/2`/`完成第 2/2`, not truncation); AT-B03a edit plan goal (200); AT-B03b confirm plan (200, PLANNING→ACTIVE). Direct MySQL SELECT confirmed stage + chapter_plan rows. Backend tests 14/14, frontend build clean, ai-service pytest 7/7.

## CRITICAL FIX — Java→Python HTTP/1.1 (TASK-017 commit)
- **Symptom:** every backend→Python POST returned HTTP 422 `loc:["body"] Field required input:null` (FastAPI saw an empty body) even though the request object serialized fine.
- **Root cause:** Spring Boot's auto-configured `RestClient.Builder` bean defaults to the JDK `java.net.http.HttpClient`, which sends `Upgrade: h2c` to negotiate HTTP/2. FastAPI/uvicorn (h11) does NOT handle the cleartext h2c upgrade and silently reads an empty body.
- **Fix:** `AiServiceClient` now builds the RestClient on an explicit `SimpleClientHttpRequestFactory` (HttpURLConnection, HTTP/1.1, `setOutputStreaming(false)`) and sends the request body as a pre-serialized JSON `String` (so `Content-Length` is always correct). Verified on the wire: request now carries `Content-Length: 449` + full JSON body + no `Upgrade` header.
- Also added `PlanStageRequestSerializationTest` (Spring `ObjectMapper` serializes the record to 190 bytes — documents the contract).

## M0 Verified Deliverables (all committed)
- **TASK-001..006** repo structure, Spring Boot 3.5.0 backend, MySQL `story_ai` DB + `story_dev` user, Python AI Service (FastAPI, 6 `/ai/*` endpoints, Mock LLM), Vue 3+TS+Vite frontend, backend↔AI connectivity check — see git log `6ad302d`..`c8f37eb`

## M3 Verified Deliverables (this commit)
- **TASK-018** Chapter min schema: `chapter` (story_id FK CASCADE, stage_id FK CASCADE, plan_id FK SET NULL + UNIQUE, chapter_number UNIQUE per story, title, content MEDIUMTEXT, summary, generation_status default 'GENERATED', timestamps); idempotent V3 migration — applied to `story_ai`.
- **TASK-019** Chapter persistence: `ChapterMapper` (insert w/ generated key, findById, findByPlanId, findByStageId, findByStoryId, maxChapterNumber) + XML; `ChapterService` (`@Transactional` saveChapter / getChapter 404 / listByStage / listByStory / nextChapterNumber). UNIQUE(plan_id) enforces one chapter per plan.
- **TASK-020** Writer AI contract: `GenerateChapterRequest` record matching Pydantic (coreIdea, constraints, stageDirection, chapterGoal, chapterOrder, currentState, storyMemories, relationshipState, recentContext) + `GenerateChapterResponse` record (title, content, summary). `AiServiceClient.generateChapter()` added using the same HTTP/1.1 String-body pattern (TASK-017 fix).
- **TASK-021** Python Writer: `generate_chapter` (mock-first `mock_generate`, real LangChain via env) + `/ai/generate-chapter` endpoint; pytest 7/7 — done in M0.
- **TASK-022** Java context assembly v1: `ChapterGenerationService.buildRequest` assembles story constraints + stage direction + plan chapter goal + previous chapter summary as recentContext. CurrentState/Memory/Relationship empty (allowed by contract, filled in M4).
- **TASK-023** Generate-one-chapter service: `ChapterGenerationService.generateNextChapter` = load context → call Python OUTSIDE tx → validate (title/content/summary non-blank) → persist. Picks next pending plan by lowest chapter order; `NoPendingChapterException` (→ 409) when none. AI call is OUTSIDE the DB transaction; persistence is `@Transactional` in `ChapterService`.
- **TASK-024** Chapter reading UI: `frontend/src/api/chapters.ts` (generateNextChapter/listStageChapters/getChapter + extractChapterError), `ChapterPanel.vue` (list chapters, expand to read content/summary, "生成下一章" button, auto-detects all-generated), wired into `StagePlanning.vue` with `:plan-count`.
- **M3 gate (AT-C01) live-verified via curl (`:8080`):** created Story(57) → Stage(29, 2 plans, PLANNING→ACTIVE) → POST `/api/stages/29/chapters` → Chapter 1 (planId 94, ch#1) 200 → Chapter 2 (planId 95, ch#2) 200 → 3rd POST → 409 `NO_PENDING_CHAPTER` (one chapter per plan enforced). Direct MySQL SELECT confirmed 2 chapter rows. Backend tests 18/18, ai-service pytest 7/7, frontend build clean (99 modules). **Java→Python HTTP/1.1 writer path re-exercised — no 422.**

## M2 Verified Deliverables (all committed)

## Environment (verified this session)
- Maven 3.9.16 via wrapper `/c/tools/mvn.sh`. Java 17 (Corretto 17.0.20). Node 22.22.2; Python 3.13.12 (venv at `C:\Users\Administrator\.workbuddy\binaries\python\envs\default`).
- MySQL 8.4.9 running; DB `story_ai` (utf8mb4); app user `story_dev` (password in local env only — NOT committed).
- **LLM API Key: NOT available.** AI Service runs in `MOCK` mode (deterministic `MockProvider`); real LangChain provider enabled via `LLM_API_KEY` env.

## Running the stack (dev)
1. Python: `cd ai-service && <venv>/Scripts/python.exe -m uvicorn app.main:app --port 8000`
2. Backend: `java -jar backend/target/story-ai-backend-0.1.0.jar --server.port=8080 --spring.profiles.active=local` (build: `/c/tools/mvn.sh clean package -DskipTests`)
3. Frontend: `cd frontend && npm run dev` (Vite `:5173`, proxies `/api` → `:8080`)
4. Tests: `DB_USERNAME=story_dev DB_PASSWORD=storypass /c/tools/mvn.sh test` (backend, 14/14); `pytest` (ai-service, 7/7); `npm run build` (frontend)

## Known Blockers / Deviations
- `MOCK` LLM: deterministic outputs until `LLM_API_KEY` supplied. Planner/Writer quality acceptance (M2+/M3+) needs a real provider — recorded, not hidden.
- Java 17 vs spec Java 21: functional deviation, no behavior impact for v0.1.
- **Sandbox port quirk:** always pass explicit `--server.port=8080` when running the backend jar (sandbox injects a random port otherwise).
- **Vite dist clean quirk:** `vite build` may fail emptying `dist/` via safe-delete shim; `rm -rf dist` first then rebuild.
- **Java→Python MUST use HTTP/1.1** (`SimpleClientHttpRequestFactory`): the JDK `java.net.http.HttpClient` upgrade to h2c breaks FastAPI. Do NOT switch the AI client back to the default RestClient.Builder bean without forcing HTTP/1.1.
- Stage status is a free string column; transitions enforced by service methods (PLANNING→ACTIVE on confirm), no branch system yet (M2 scope).

## Next Safe Action
Start M4: TASK-025 design minimum Memory schema (MemoryCandidate / StoryMemory / CurrentState / RelationshipState; AUTO/REVIEW/IGNORE; sourceChapterId/evidenceText), TASK-026 memory persistence, TASK-027 memory extraction contract, TASK-028 Python extractor, TASK-029 candidate processing, TASK-030 current state rules, TASK-031 relationship state, TASK-032 connect chapter→memory, TASK-033 add memory to writer context, TASK-034 review API, TASK-035 memory UI. Then M4 gate (Generate→Save→Extract→Save Candidates→Apply AUTO→Checkpoint).
