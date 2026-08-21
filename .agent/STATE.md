# STATE — AI Story Co-Author v0.1

_Updated: 2026-08-21 (M2 complete — Stage Planning vertical slice live-verified)_

## Current Project State
- **Current Milestone:** `M2 — Stage Planning Vertical Slice` → **COMPLETE**
- **Current Task:** M2 gate closed; all of TASK-001..TASK-017 → `DONE`
- **Next Task:** `M3 — Single Chapter Generation` → start at `TASK-018 — Add Chapter Minimum Schema`

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
Start M3: TASK-018 design minimum Chapter schema (story/chapter_plan/chapter_number/title/content/summary/status), then TASK-019 chapter persistence, TASK-020 writer AI contract, TASK-021 Python writer, TASK-022 Java context assembly, TASK-023 generate-one-chapter service, TASK-024 chapter reading UI. Then M3 gate (Story→Stage→Plan→Generate One Chapter→Save→Read in Vue).
