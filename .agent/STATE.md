# STATE — AI Story Co-Author v0.1

_Updated: 2026-08-21 (M1 complete — Story vertical slice live-verified)_

## Current Project State
- **Current Milestone:** `M1 — Story Vertical Slice` → **COMPLETE**
- **Current Task:** M1 gate closed; all of TASK-001..TASK-010 → `DONE`
- **Next Task:** `M2 — Stage Planning Vertical Slice` → start at `TASK-011 — Add Stage and ChapterPlan Minimum Schema`

## M1 Verified Deliverables (all committed)
- **TASK-007** minimum Story schema: `story` + `story_constraint` tables (idempotent V1 migration) — `97e3fef`
- **TASK-008** MyBatis persistence: StoryMapper/StoryConstraintMapper (interface + XML, generated keys, batch insert) + StoryService (`@Transactional` create, get, list) — `fa7958b`
- **TASK-009** Story REST API: `POST/GET/GET` `/api/stories` + DTO boundary + GlobalExceptionHandler (400 VALIDATION_ERROR + fieldErrors / 404 NOT_FOUND / 500) — `bcb4a92`
- **TASK-010** Story Creation UI: typed API client, CreateStoryView (name/coreIdea/constraints rows/optional stage direction), StoryListView, StoryDetailView, Pinia store, routes `/create` `/stories` `/stories/:id` — `f6bb237` (includes controller fix: 201 response re-reads DB state — status=ACTIVE + timestamps)
- **M1 gate (AT-A01)**: live-verified the full chain Vue → Spring Boot → MyBatis → MySQL → Spring Boot → Vue. Create via Vite proxy (`:5173/api`) → row in MySQL (`SELECT` confirmed story + 3 constraints, sortOrder 0/1/2) → refresh GET returns full data. Error paths verified: empty name → 400 + fieldErrors; missing id → 404.

## M0 Verified Deliverables (all committed)
- **TASK-001..006** repo structure, Spring Boot 3.5.0 backend, MySQL `story_ai` DB + `story_dev` user, Python AI Service (FastAPI, 6 `/ai/*` endpoints, Mock LLM), Vue 3+TS+Vite frontend, backend↔AI connectivity check — see git log `6ad302d`..`c8f37eb`

## Environment (verified this session)
- Maven 3.9.16 via wrapper `/c/tools/mvn.sh` (stock `mvn` broken under Git Bash). Use this wrapper for all Maven calls.
- Java: **17 (Corretto 17.0.20)**. Spec asks Java 21; deviation recorded — no behavior impact for v0.1.
- Node 22.22.2 (managed); Python 3.13.12 (managed), venv at `C:\Users\Administrator\.workbuddy\binaries\python\envs\default`.
- MySQL 8.4.9 running; DB `story_ai` (utf8mb4); app user `story_dev` (password in local env only — NOT committed).
- **LLM API Key: NOT available.** AI Service runs in `MOCK` mode (deterministic `MockProvider`); real LangChain provider enabled via `LLM_API_KEY` env. Acceptance narrative-quality tests will need a real key.

## Running the stack (dev)
1. Python: `cd ai-service && <venv>/Scripts/python.exe -m uvicorn app.main:app --port 8000`
2. Backend: `java -jar backend/target/story-ai-backend-0.1.0.jar --server.port=8080 --spring.profiles.active=local` (build: `/c/tools/mvn.sh clean package -DskipTests`)
3. Frontend: `cd frontend && npm run dev` (Vite `:5173`, proxies `/api` → `:8080`)
4. Tests: `DB_USERNAME=story_dev DB_PASSWORD=storypass /c/tools/mvn.sh test` (backend, 8/8); `pytest` (ai-service, 7/7); `npm run build` (frontend)

## Last Verified Commit
`f6bb237 M1/TASK-010: Story Creation UI + create-response DB state fix`

## Known Blockers / Deviations
- `MOCK` LLM: deterministic outputs until `LLM_API_KEY` supplied. Planner/Writer quality acceptance (M2+) needs a real provider — recorded, not hidden.
- Java 17 vs spec Java 21: functional deviation, no behavior impact for v0.1.
- **Sandbox port quirk:** always pass explicit `--server.port=8080` when running the backend jar (sandbox injects a random port otherwise).
- **Vite dist clean quirk:** `vite build` may fail emptying `dist/` via safe-delete shim; `rm -rf dist` first then rebuild.
- Story status is a free string column defaulting to `ACTIVE`; no status transition enforcement yet (fine for v0.1 — status transitions arrive with Stage workflows in M2).

## Next Safe Action
Start M2: TASK-011 design minimum Stage + ChapterPlan schema (only what the planning flow needs), then TASK-012 Stage persistence, TASK-013 Java↔Python planner contract (reuse existing `/ai/plan-stage` DTOs), TASK-014 planner service (mock-first), TASK-015 Spring planning service, TASK-016 replan, TASK-017 planning UI.
