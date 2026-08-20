# .agent/STATE.md

## Current Project State

Project:

**AI Story Co-Author v0.1**

Current Milestone:

`M0 — Repository Bootstrap`

Current Task:

`TASK-001 — Initialize Repository Structure`

Task Status:

`NOT_STARTED`

---

## Last Verified Commit

`NONE — implementation has not started`

---

## Last Successful Verification

`NONE — repository implementation has not started`

---

## Current Working Tree

Expected state:

> No implementation code exists yet.

Before starting work, run:

```bash
git status
git log --oneline -5
```

If actual repository state differs from this file:

> trust Git and inspected code over this stale statement, then update this file before continuing.

---

## Human-Owned Specifications

The following documents define the current v0.1 boundaries and MUST NOT be silently modified by Coding Agents:

```text
PROJECT_VISION.md
MVP_SCOPE.md
PRODUCT_SPEC.md
ACCEPTANCE_TESTS.md
TECH_STACK.md
ARCHITECTURE.md
AGENTS.md
```

---

## Current Technical Direction

Frontend:

```text
Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios
```

Main Backend:

```text
Java 21
Spring Boot 3.5.x
MyBatis
MySQL 8.4 LTS
Maven
```

AI Service:

```text
Python 3.12+
FastAPI
Pydantic
LangChain
One LLM Provider
```

---

## Core Architecture Rule

> **AI proposes. Java decides. MySQL remembers.**

Python owns AI computation.

Spring Boot owns business logic and business state transitions.

MySQL is the persistent business source of truth.

Vue must access product business capabilities through Spring Boot.

---

## v0.1 Core Mission

Validate:

> Whether an author can provide only stage-level story direction while the system uses Story State and Story Memory to autonomously expand that direction into multiple continuous chapters without requiring the author to repeatedly provide full background context.

---

## Current Scope Guard

Do NOT add during v0.1 unless Human Authority explicitly changes the scope:

```text
Redis
Kafka
RabbitMQ
RocketMQ
Spring Cloud
Spring AI
JPA / Hibernate
MyBatis-Plus
LangGraph
Vector Database
RAG
Knowledge Graph
Temporal Memory Engine
Canon Governance
Kubernetes
Multi-user authentication
```

---

## Current Known Blockers

`NONE`

---

## Current Known Failures

`NONE — implementation has not started`

---

## Currently Modified Files

`NONE EXPECTED`

If `git status` reports modifications:

> inspect them before performing any implementation.

Do not discard unknown changes automatically.

---

## Recovery Instructions

If a new Agent starts with no reliable previous-session context:

```text
1. Read AGENTS.md
2. Read this file
3. Read .agent/TASKS.md
4. Run git status
5. Run git diff
6. Run git log --oneline -5
7. Compare repository reality with this STATE
8. Run the smallest relevant verification
9. Update this file if stale
10. Resume the current task
```

---

## Next Safe Action

Start:

`TASK-001 — Initialize Repository Structure`

Expected first structure:

```text
backend/
ai-service/
frontend/
.agent/
```

Then proceed through `M0` in `.agent/TASKS.md`.

Do not design all database tables during repository initialization.

Do not implement optional infrastructure.

---

## State Update Requirement

The Coding Agent MUST update this file when:

- a new Task becomes `IN_PROGRESS`;
- a Task becomes `DONE`;
- verification succeeds or fails;
- a Blocker appears;
- important files remain partially modified;
- the session may end before the current Task completes.

Keep this document:

> short, factual, current.

Historical narration belongs elsewhere, not here.