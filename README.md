# AI Story Co-Author v0.1

> **Status: ACCEPTED (2026-08-21)** — core product loop complete, 5-chapter end-to-end
> verified, structured memory observable, Baseline/Memory experiment run, failures recorded.

An AI-assisted co-writing tool for long-form serialized fiction. The human authors the
Story, Stage Directions, and Constraints; the system proposes chapter plans, generates
chapters, extracts structured memory, and answers story queries.

**Architecture rule (from `AGENTS.md`):** *AI proposes. Java decides. MySQL remembers.*
Vue never calls Python directly — all AI goes `Vue → Spring Boot → Python AI Service`.

## Tech stack

| Layer | Tech |
|-------|------|
| Frontend | Vue 3 + TypeScript + Vite + Router + Pinia + Axios |
| Backend | Java 17 + Spring Boot 3.5 + MyBatis + MySQL 8.4 |
| AI Service | Python 3.13 + FastAPI + Pydantic + (optional) LangChain |
| Tests | JUnit 5 (backend) · pytest (AI) · Vite build (frontend) |

## What v0.1 does (verified)

- **Story** vertical slice: create / read / list stories with constraints.
- **Stage Planning**: one direction → structured chapter plan (mock planner deterministic).
- **Single & Multi-Chapter Generation**: CONTINUOUS (auto) and STEP-by-step (author clicks Continue) modes over the same reusable unit.
- **Memory**: per-chapter extraction of Current State / Relationship / Story Memory candidates with AUTO / REVIEW / IGNORE processing; author can apply or ignore.
- **Author Assistance**: Story Query (location / inventory / relationship / unknown) and Planner Suggestions (3 distinct directions).
- **Failure handling**: generation job tracks PENDING/RUNNING/PAUSED/COMPLETED/FAILED; AI-down → FAILED with captured error, safe Retry re-runs the same pending plan without duplicate chapters.

## Acceptance evidence

| Item | Where |
|------|-------|
| Acceptance Story | `ACCEPTANCE_TESTS.md` §5/§6 |
| 5 continuous chapters | M7 end-to-end run (`scripts/m7_e2e.sh`), Story 200 / Stage 122 |
| Stage Plan | `chapter_plan` rows |
| Story Constraints | `story_constraint` rows |
| Current State example | `current_state`: 禁书区最深处 / 生锈的铜钥匙 / 受伤 |
| Story Memory example | `story_memory` (populated on real-LLM run) |
| REVIEW example | `memory_candidate` (RELATIONSHIP, PENDING→APPLIED by author) |
| Source Evidence | `memory_candidate.evidence` + `source_chapter_id` |
| Story Query example | "当前位置：禁书区最深处。" / "当前持有：生锈的铜钥匙。" |
| Planner Suggestions | 冲突型 / 成长型 / 悬疑型 |
| Baseline vs Memory | `.agent/EXPERIMENT.md` |
| Known Issues | `.agent/EXPERIMENT.md` → v0.1 Known Issues |

## Five-Chapter PASS criteria (PASS-01..12)

All 12 passed in the M7 live run (see `.agent/EXPERIMENT.md`). Notably:
PASS-01 (5 chapters), PASS-03 (direction→multi-chapter), PASS-04/06 (Current State in
generation + applied), PASS-07/08 (author apply/override REVIEW/IGNORE), PASS-10 (Story
Query), PASS-11 (3 suggestions), PASS-12 (both generation modes).

## Running locally

See **[RUN.md](RUN.md)** for the full step-by-step (MySQL setup, three services, tests).

Quick start:

```bash
# 1. AI service
cd ai-service && <venv>/Scripts/python.exe -m uvicorn app.main:app --port 8000
# 2. Backend
cd backend && /c/tools/mvn.sh clean package -DskipTests && \
  java -jar target/story-ai-backend-0.1.0.jar --server.port=8080 --spring.profiles.active=local
# 3. Frontend
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## Known limitations (not v0.1 failures)

- **Mock LLM**: with no `LLM_API_KEY`, the writer is deterministic and self-contained, so
  prose-level memory benefit (Baseline vs Memory) is demonstrated only at the structured
  memory + Story Query layer, not in chapter text. A real LangChain provider is needed for
  full prose comparison.
- **Foreshadowing (STORY_MEMORY)** candidates are not produced by the mock extractor for the
  default generated text; a real LLM populates them.
- Java 17 used (spec says 21) — no behavior impact.
- Not implemented (deferred, per `AGENTS.md`): RAG, vector search, temporal memory, canon
  governance, auth/multi-user, Docker polish.

## Documentation

- `AGENTS.md` — operational protocol & agent workflow (authoritative).
- `MVP_SCOPE.md` / `PRODUCT_SPEC.md` / `ACCEPTANCE_TESTS.md` — human-owned specs.
- `ARCHITECTURE.md` / `TECH_STACK.md` / `PROJECT_VISION.md` — design references.
- `.agent/STATE.md` / `.agent/TASKS.md` — milestone & task tracking.
- `.agent/EXPERIMENT.md` — M7 acceptance + memory experiment record.
- `RUN.md` — how to run locally.
