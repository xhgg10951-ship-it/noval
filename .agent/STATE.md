# STATE — AI Story Co-Author v0.1

_Updated: 2026-08-20 (M0 complete — BuildLead autonomous session)_

## Current Project State
- **Current Milestone:** `M0 — Repository Bootstrap` → **COMPLETE**
- **Current Task:** M0 gate closed; all of TASK-001..TASK-006 → `DONE`
- **Next Task:** `M1 — Story Vertical Slice` → start at `TASK-007 — Design Minimum Story Schema`

## M0 Verified Deliverables (all committed)
- **TASK-001** repo structure (`backend/ ai-service/ frontend/ .agent/`), `.gitignore` — `6ad302d`
- **TASK-002** Spring Boot 3.5.0 backend (web/validation/MyBatis/MySQL/test), `/api/health` — `26170c7`
- **TASK-003** MySQL `story_ai` DB + `story_dev` user; env-based config (no committed passwords); `DatabaseConnectionTest` (gated on `DB_PASSWORD`) — `cf2a42c`
- **TASK-004** Python AI Service (FastAPI, 6 `/ai/*` endpoints, pluggable Mock/LangChain provider, pytest 7/7) — `2bb70fc`
- **TASK-005** Vue 3 + TS + Vite + Router + Pinia + Axios; `npm run build` passes — `122739c`
- **TASK-006** `AiServiceClient` (RestClient) calls Python `/health`; live verified Java→Python boundary — `8e205ef`

## Environment (verified this session)
- Maven 3.9.16 via wrapper `/c/tools/mvn.sh` (stock `mvn` broken under Git Bash). Use this wrapper for all Maven calls.
- Java: **17 (Corretto 17.0.20)**. Spec asks Java 21; Spring Boot 3.5.x runs on 17. Deviation recorded — runnable now.
- Node 22.22.2 (managed) + npm; Python 3.13.12 (managed) with venv at `C:\Users\Administrator\.workbuddy\binaries\python\envs\default`.
- MySQL 8.4.9 running; DB `story_ai` (utf8mb4); app user `story_dev` (password in local env only — NOT committed).
- **LLM API Key: NOT available.** AI Service runs in `MOCK` mode (deterministic `MockProvider`); real LangChain provider enabled via `LLM_API_KEY` env. Acceptance quality tests (AT-*) will need a real key.

## Last Verified Commit
`8e205ef M0/TASK-006: backend<->AI service connectivity check`

## Known Blockers / Deviations
- `MOCK` LLM: deterministic outputs until `LLM_API_KEY` supplied. Acceptance narrative-quality tests need a real provider — recorded, not hidden.
- Java 17 vs spec Java 21: functional deviation, no behavior impact for v0.1.
- **Sandbox port quirk:** in this sandbox, running the backend jar without an explicit `--server.port=8080` arg yields a randomized port (sandbox injects a port system property). Always pass `--server.port=8080` (or run outside the sandbox). The project's `application.yml` correctly sets 8080; this is an execution-environment detail, not a code bug.

## Next Safe Action
Start M1: TASK-007 design minimum Story schema (Story + StoryConstraints tables only), then TASK-008 MyBatis persistence, TASK-009 Story REST API (Create/Get/List), TASK-010 Story Creation UI. M1 gate proves Vue → Spring Boot → MyBatis → MySQL → Spring Boot → Vue.
