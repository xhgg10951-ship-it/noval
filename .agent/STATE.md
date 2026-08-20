# STATE — AI Story Co-Author v0.1

_Updated: 2026-08-20 (BuildLead autonomous session start)_

## Current Project State
- **Current Milestone:** `M0 — Repository Bootstrap`
- **Current Task:** `TASK-004 — Initialize Python AI Service` → `DONE` (verified: pytest 7/7 + live HTTP /health & /ai/plan-stage on :8011, MOCK mode)
- **Next Task:** `TASK-002 — Initialize Spring Boot Backend` (then TASK-003 MySQL, TASK-005 Vue, TASK-006 connectivity)

## Environment (verified this session)
- Maven 3.9.16 fixed via wrapper `/c/tools/mvn.sh` (stock `mvn` is broken under Git Bash).
- Java: **17 (Corretto 17.0.20)**. Spec asks Java 21; Spring Boot 3.5.x runs on 17. Recorded as a deviation (Tech Change) — runnable now, revisit for 21 later.
- Node 22.22.2 + npm 10.9.7 available (Vue build OK).
- Python 3.13.12 available (`pytest` OK).
- MySQL 8.4 present (`mysqld` available) — local DB can be started.
- **LLM API Key: NOT available.** Per AGENTS.md §55, AI Service implements a pluggable `LLMProvider` with a deterministic `MockProvider` so the full generation pipeline runs and is testable without a key. Marked `MOCK` below. Real provider enabled via `LLM_API_KEY` env when available.

## Last Verified Commit
`8e5dbbb docs: define v0.1 product and agent workflow` (pre-implementation)

## Current Working Tree
- Repo skeleton created: `backend/`, `ai-service/`, `frontend/`, `.agent/` (canonical `TASKS.md`/`STATE.md` after rename from `agent-*.md`).
- No product code yet.

## Known Blockers / Deviations
- `MOCK` LLM: all AI outputs are deterministic mock until a real `LLM_API_KEY` is supplied. Acceptance quality tests (AT-*) need a real provider — recorded, not hidden.
- Java 17 vs spec Java 21: functional deviation, no behavior impact for v0.1.

## Next Safe Action
Continue M0: build Spring Boot backend (TASK-002), MySQL config (TASK-003), Python AI Service (TASK-004), Vue frontend (TASK-005), then backend↔AI connectivity (TASK-006). Then M1 Story vertical slice.
