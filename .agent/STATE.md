# .agent/STATE.md

> Project: **AI Story Co-Author**
>
> Active Version: **v0.1.1**
>
> State Type: **RELEASE HARDENING STATE**
>
> Updated: **2026-08-23**

# 1. Current Project State

Current Version:

`v0.1.1`

Requirement Status:

`FROZEN`

Frozen Requirement Document:

`V0.1.1_RELEASE_HARDENING.md` (highest current product requirement)

Current Implementation Plan:

`.agent/TASKS.md`

Current Phase:

`RELEASE HARDENING — RH-04 COMPLETE`

Current Task:

`RH-05 — Context Hardening (NEXT)`

Task Status:

`Feature Complete, Release Hardening Required — v0.1.1 is NOT ACCEPTED until RH-01~RH-10 and the final qwen3.7-plus suite pass`

Task Evidence:

- RH-01 DONE (2026-08-23) — regression first reproduced stale summary after
  Manual Edit; fixed Manual Edit summary refresh + automatic re-extract,
  Regenerate title/content/summary synchronization, and Polish summary refresh
  with title preservation.
- AC-H01 Engineering Verification: PASSED —
  `ChapterRevisionIntegrationTest` 8/8; Python tests 8/8.
- AC-H01 Real-LLM Semantic Verification: NOT_REQUIRED (consistency/plumbing;
  final AI semantics are re-run together at RH-10 with qwen3.7-plus).
- RH-02 DONE (2026-08-23) — V15 adds candidate provenance to current and
  relationship live state; every upsert replaces provenance, and revision
  invalidation now uses the Apply path's normalized slot plus source ownership.
- AC-H02 Engineering Verification: PASSED — both regression cases failed before
  the fix, then `MemoryProvenanceIntegrationTest` passed 2/2; related Memory and
  Revision integration tests passed 16/16.
- AC-H02 Real-LLM Semantic Verification: NOT_REQUIRED (deterministic persistence
  and invalidation behavior).
- RH-03 DONE (2026-08-23) — Replan Remaining now offsets fresh relative Planner
  items from the latest real story chapter, not the maximum superseded plan
  order; planVersion remains the history separator.
- AC-H03 Engineering Verification: PASSED — the new 9-plan regression failed
  with V2 orders 10/11/12 before the fix and passed with active orders 4/5/6,
  generated chapter numbers 4/5/6, and Writer chapterOrder 4/5/6 after it.
- AC-H03 Real-LLM Semantic Verification: NOT_REQUIRED here; final AC-106 semantic
  behavior is re-run with qwen3.7-plus at RH-10.
- RH-04 DONE (2026-08-23) — GenerationPanel polls PENDING/RUNNING jobs every
  1500ms, stops at every terminal state, clears timers across Stage changes and
  unmount, and exposes Pause/Stop with a safe-checkpoint wait state.
- AC-H04/H05/H06 Engineering Verification: PASSED — frontend Vitest 5/5,
  production build PASSED, backend Pause/Stop checkpoint regressions 2/2.
- AC-H04/H05/H06 Real-LLM Semantic Verification: NOT_REQUIRED.
- Historical TASK-101~179 remain implementation history only and do not override
  the Release Hardening gate.

---

# 2. v0.1 Truthful Status

v0.1 should currently be treated as:

> **Engineering Pipeline Accepted**

v0.1 should NOT be treated as:

> **Real-LLM Product Behavior Accepted**

The following engineering paths exist and have been demonstrated:

```text
Vue
→ Spring Boot
→ MyBatis
→ MySQL
```

and:

```text
Spring Boot
→ Python AI Service
→ LLM Provider
```

The repository also contains working concepts for:

- Story;
- Stage;
- ChapterPlan;
- Chapter;
- GenerationJob;
- MemoryCandidate;
- CurrentState;
- RelationshipState;
- StoryMemory;
- Story Query;
- Planner Suggestions;
- Continuous / Step generation.

However:

> ~~real-LLM semantic quality and long-form authoring behavior are not accepted yet.~~
> **SUPERSEDED 2026-08-22**: v0.1.1 completed the Real-LLM acceptance suite
> (6/7 first-pass + AC-103 PASS after model upgrade) — see §20 Final Current State.
> This section is retained as historical v0.1 baseline context.

### 2.1 Status Corrections Applied (TASK-102)

The following inaccurate v0.1 claims are corrected / superseded as of this
baseline. Historical files under `.agent/history/` and `docs/history/` are
evidence only and are NOT edited; the truthful status above is authoritative.

1. **"Writer used Structured Memory"** — FALSE. Active code
   (`ChapterGenerationService.buildRequest`) sends empty `currentState` /
   `storyMemories` / `relationshipState` (proven in
   `.agent/EVIDENCE_v0.1_REGRESSION.md` §3, RC-02). Any prior Agent statement
   claiming Writer Memory integration was complete is stale / incorrect.
2. **"Baseline vs Memory real-prose experiment completed"** — FALSE. No
   real-LLM prose comparison was ever executed; the historical experiment only
   observed structured Current State / Story Memory *rows* under Mock and
   recorded prose comparison as a known limitation. It did NOT prove Memory
   improves generated prose.
3. **"Mock PASS = Semantic PASS"** — FALSE. Mock only proves HTTP / DTO /
   parsing / persistence / context-wiring / workflow / state machine. It can
   never prove writing quality, Planner continuation, length control, Memory
   benefit in prose, or long-form pace (see AC-117).

---

# 3. Why v0.1.1 Exists

Real use of v0.1 exposed the following problems:

1. chapters are often around 500 characters and no target length exists;
2. prose has strong AI-like / mechanical writing characteristics;
3. confirmed Stage plans cannot safely be changed mid-generation;
4. generated chapters cannot be manually edited, regenerated, or polished;
5. there is no long-form pace model for a novel targeting hundreds of chapters;
6. generated content can diverge from the Chapter Plan;
7. low-value details can become self-reinforcing narrative anchors;
8. later Stage planning may restart the story instead of continuing it;
9. Memory quality, deduplication, importance, and scope are insufficient;
10. generation recovery has workflow consistency problems.

These are now covered by the frozen v0.1.1 plan.

---

# 4. Verified Repository Findings

The following are **confirmed repository facts**, not design hypotheses.

## 4.1 Planner Context Is Not Fully Wired

Current Stage planning code has an AI contract capable of carrying story context, but the active Java request path has been observed sending empty values for important existing-story context.

Impact:

> Later Stage planning can behave like a fresh story plan.

v0.1.1 Phase 1 must fix this.

---

## 4.2 Writer Structured Memory Is Not Fully Wired

The Writer request contract contains:

```text
currentState
storyMemories
relationshipState
```

but the current Java chapter-generation path has been observed sending empty lists for them.

Impact:

> Structured Memory has not yet been proven to affect generated prose.

Any previous Agent-owned statement claiming Writer Memory integration was complete must be treated as stale / incorrect until repository behavior proves otherwise.

---

## 4.3 Previous Summary Is a Major Continuity Input

Current chapter generation relies heavily on previous chapter summary context.

Observed real-world behavior suggests a possible loop:

```text
low-value detail appears
→ summary repeats it
→ next chapter receives summary
→ detail appears again
→ new summary reinforces it
```

This is currently a stronger explanation for the repeated “bread” behavior than direct StoryMemory injection into Writer.

Memory classification pollution remains a separate issue.

---

## 4.4 Chapter Planning Data Is Underused

Current planning contains more information than the Writer actually consumes.

In particular:

> planning progress information is not fully carried into the Writer execution path.

v0.1.1 expands the model into ChapterSpec and requires end-to-end field preservation.

---

## 4.5 No Chapter Length Contract Exists

The current v0.1 model does not define a reliable:

```text
targetCharacters
```

path from Story / Plan → Writer.

Therefore short output is expected behavior under the current implementation, not merely an isolated model failure.

---

## 4.6 Chapter Editing Is Not Implemented

Current chapter workflow is primarily:

```text
Generate
Read
```

There is no complete author workflow for:

```text
Manual Edit
Regenerate
AI Polish
Revision History
```

v0.1.1 adds this explicitly.

---

## 4.7 Existing Full Replan Is Unsafe After Generation Starts

The current v0.1 replan behavior replaces the Stage plan wholesale.

That design is suitable only before generation begins.

It is NOT safe to expose unchanged for an ACTIVE Stage because completed Chapter ↔ Plan history may be broken.

v0.1.1 must implement:

> Replan Remaining

not simply enable the old Replan UI.

---

## 4.8 Memory Extraction Retry Has a Known Recovery Hole

Current flow conceptually performs:

```text
Generate Chapter
→ Persist Chapter
→ Extract Memory
```

If:

```text
Chapter persisted
+
Memory extraction fails
```

the current generation recovery logic may treat the already-persisted Chapter as completed generation work and advance to a later plan rather than retrying extraction for the same chapter.

v0.1.1 Phase 3 must fix this.

---

## 4.9 Continuous Generation Is Currently Request-bound

Current Continuous generation behavior is implemented as a synchronous multi-step operation in the request flow.

Impact:

- real-time progress is limited;
- Pause / Stop are not reliable product controls;
- mid-run Replan is not safely supported;
- long LLM calls can keep the request open.

v0.1.1 will move Continuous execution to an application-managed background executor.

No MQ is required.

---

## 4.10 Stage Completion Lifecycle Is Incomplete

GenerationJob can complete while Stage lifecycle does not necessarily transition cleanly to:

`COMPLETED`

v0.1.1 must make Stage completion explicit.

---

## 4.11 Memory Contract Is Too Permissive

Current Memory typing is too open-ended and long-term StoryMemory persistence lacks sufficient:

```text
importance
scope
deduplication
```

v0.1.1 introduces Memory v2.

---

## 4.12 Inventory Representation Is Insufficient for Multiple Items

A single generic inventory state slot can cause one acquired item to overwrite another.

v0.1.1 must support multiple items with a minimal representation.

---

# 5. Acceptance Truth

The current repository contains Mock-based engineering tests and end-to-end plumbing verification.

These are valuable for:

- HTTP;
- DTO;
- persistence;
- structured parsing;
- workflow;
- state transitions;
- contract wiring.

They are NOT sufficient evidence for:

- prose quality;
- long-form pacing;
- Planner continuation quality;
- Writer goal adherence;
- actual Memory benefit in prose;
- Polish quality;
- low-value detail isolation.

From v0.1.1 onward:

> **Mock proves plumbing. Real LLM proves AI behavior.**

---

# 6. Current Architecture Direction

The architecture remains:

```text
Vue 3
↓
Spring Boot
├── MyBatis → MySQL
└── HTTP/JSON → Python AI Service
                  └── LLM Provider
```

Core rule:

> **AI proposes. Java decides. MySQL remembers.**

v0.1.1 adds:

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

---

# 7. Current Technical Stack

Frontend:

```text
Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios
```

Backend:

```text
Spring Boot 3.5.x
MyBatis
MySQL 8.4
Maven
```

AI Service:

```text
Python
FastAPI
Pydantic
LangChain
One LLM Provider
```

Important repository reality:

The current backend has been run with Java 17 even though the earlier technology specification targeted Java 21.

This existing deviation is not a v0.1.1 product blocker.

Do NOT spend v0.1.1 scope on upgrading Java solely to match the old document.

---

# 8. v0.1.1 Frozen Scope

v0.1.1 MUST implement:

```text
Planner Continuation Context

Writer Context Wiring

ChapterSpec

Chapter Length Control

Replan Remaining

Chapter Revision

Manual Edit

Regenerate

AI Polish

Revision → Memory Refresh

targetChapterCount

Arc

Long-form Pace Guard

Memory v2

Memory Dedup v1

Inventory Multi-item

Extraction Retry Fix

Async Continuous Generation

Pause

Stop

Stage Completion

Real-LLM Semantic Acceptance
```

---

# 9. Explicitly Deferred

Do NOT add during v0.1.1:

```text
RAG
Embedding
Vector Database
GraphRAG
Knowledge Graph
Temporal Truth Engine
Canon Governance
LangGraph
Spring AI
Redis
Kafka
RabbitMQ
RocketMQ
Spring Cloud
Kubernetes
Volume layer
Authentication
Multi-user
Payment
Auto Publishing
AI Detection Bypass
Fine-tuning
Automatic Multi-Agent Story Room
```

If a new request appears:

> record it for v0.1.2 / v0.2 unless it blocks a frozen acceptance test.

---

# 10. Current Working Tree Expectations

Before starting TASK-101, the Coding Agent MUST run:

```bash
git status
git diff
git log --oneline -5
```

Do not assume this file perfectly reflects the local working tree.

If repository reality differs:

1. inspect existing modifications;
2. determine whether they are user changes, prior Agent work, or generated artifacts;
3. update this STATE file;
4. continue from the smallest safe action.

Never discard unknown changes automatically.

---

# 11. Current Known Blockers

`REAL-LLM CREDENTIAL — RESOLVED (as of 2026-08-22)`

TASK-112 / TASK-121 / TASK-122 were previously BLOCKED on a real LLM credential.
As of this session, the environment exposes:

```text
API_KEY      = sk-ws-...          (valid)
API_URL      = https://ws-...cn-beijing.maas.aliyuncs.com/compatible-mode/v1
MODEL        = qwen3-8b           (read by config.py as llm_model)
```

`api-service/app/config.py` resolves `llm_api_key` from `API_KEY` and
`llm_base_url` from `API_URL`, so `settings.using_mock_llm == False`. A live
connectivity probe succeeded (HTTP 200, qwen3-8b responded). Therefore the
frozen-requirement-defined Blocker is **resolved**, and AC-101 / AC-103 / AC-104
are now runnable.

Remaining LLM-gated acceptances further downstream (AC-106 / 107 / 108 / 114 /
117) are also unblocked by this same credential and will be executed at their
respective phases.

No fabricated Semantic PASS was ever recorded during the blocked period — the
agent continued downstream engineering per the Recovery Protocol.

---

# 12. Current Known Failures / Risks

Known:

```text
Planner continuation context incomplete

Writer structured state/memory wiring incomplete

Chapter length uncontrolled

ChapterSpec insufficient

Chapter editing unavailable

Unsafe mid-stage full replan

Memory extraction retry hole

Continuous request-bound execution

Stage completion lifecycle incomplete

Memory type/importance/scope insufficient

StoryMemory dedup insufficient

Inventory multi-item model insufficient

Mock semantic acceptance problem
```

These are expected v0.1.1 work items, not reasons to redesign the entire system.

---

# 13. Current Agent Task Source

Active task plan:

`.agent/TASKS.md`

Expected task range:

```text
TASK-101
through
TASK-179
```

Current:

```text
Phase 2 (engineering DONE)
TASK-112 AC-101 — UNBLOCKED, queued to run (real LLM reachable)
TASK-121 AC-103 — UNBLOCKED, queued to run
TASK-122 AC-104 — UNBLOCKED, queued to run
```

Do not resume old v0.1 TASK-001 ~ TASK-056 as active work.

They are historical implementation tasks only.

---

# 14. Current Phase

## Phase 0 — Correct Evidence Base

Purpose:

> establish a truthful regression baseline before modifying behavior.

Expected Phase 0 tasks:

```text
TASK-101
Snapshot v0.1 Regression Evidence

TASK-102
Correct v0.1 Status Claims

TASK-103
Capture Current AI Request Payloads

TASK-104
Phase 0 Evidence Review
```

---

# 15. Next Safe Action

Start:

> `TASK-101 — Snapshot v0.1 Regression Evidence`

Before changing Planner, Writer, Memory, database schemas, or UI:

1. inspect Git;
2. preserve current real-v0.1 examples;
3. capture current Prompt / Request behavior;
4. record current model configuration where safely observable;
5. update `.agent/STATE.md`;
6. commit the evidence checkpoint.

Do NOT begin a broad refactor before TASK-101 through TASK-104 are complete.

---

# 16. Required Verification Format Going Forward

For every AI-related implementation task, STATE updates must record:

```text
Engineering Verification:
PASSED / FAILED / NOT_RUN

Real-LLM Semantic Verification:
PASSED / FAILED / NOT_REQUIRED / BLOCKED
```

Examples:

```text
TASK-110 Writer Context Wiring

Engineering Verification:
PASSED
- Captured request contains non-empty currentState
- Captured request contains StoryMemory
- Captured request contains RelationshipState

Real-LLM Semantic Verification:
NOT_REQUIRED
```

and:

```text
TASK-112 Planner Continuation

Engineering Verification:
PASSED

Real-LLM Semantic Verification:
PASSED
- Model: ...
- Run ID: ...
- New Stage continued from existing residence
- No repeated crossing / first meeting
```

---

# 17. State Update Rules

Update this file when:

- task starts;
- task completes;
- Phase Gate passes/fails;
- test fails;
- Real-LLM acceptance fails;
- new blocker appears;
- significant working-tree modifications remain;
- schema migration changes;
- the development session may end.

Keep this file:

> current and factual.

Do NOT turn it into a full historical diary.

Detailed experiment results belong in:

`.agent/EXPERIMENT.md`

or the equivalent active experiment record.

---

# 18. DONE Integrity Rule

From v0.1.1 onward:

```text
Code exists
≠
Feature works
```

```text
DTO exists
≠
Context is wired
```

```text
Mock passes
≠
AI behavior passes
```

```text
STATE says DONE
≠
Repository proves DONE
```

Repository behavior and executed verification are authoritative.

---

# 19. Development Recovery Protocol

If a new Coding Agent enters with no reliable session memory:

```text
1. Read AGENTS.md
2. Read V0.1.1_IMPROVEMENT_PLAN.md
3. Read this STATE.md
4. Read .agent/TASKS.md
5. Run git status
6. Run git diff
7. Run git log --oneline -5
8. Inspect current Task files
9. Run smallest relevant verification
10. Reconcile STATE with repository reality
11. Resume the smallest safe next action
```

Do not ask the project owner where development stopped unless repository evidence cannot resolve it.

---

# 20. Final Current State

```text
Version:
v0.1.1

Requirements:
FROZEN

Implementation:
IN PROGRESS (Phase 3 engineering DONE; TASK-132 regression suite pending;
Phase 4 backend partially landed)

Current Phase:
v0.1.1 ACCEPTED — Phase 9 COMPLETE (BLOCKER-1 resolved via model upgrade qwen3.7-plus)

Current Task:
NONE — v0.1.1 frozen as ACCEPTED

Current Task Status:
ACCEPTED (all 79 tasks terminal; DoD fully green)

Phase 0 Gate:
PASSED (2026-08-21, after commit 63781dd)

Phase 1 Gate:
Engineering PASSED; AC-101 real-LLM PASS (TASK-112)

Phase 2 Gate:
AC-103 honest FAIL (model length ceiling, remediation recorded);
AC-104 PASS; engineering complete

Phase 3 Gate:
PASSED (2026-08-22) — TASK-132 regression suite, full mvn test 34/34.
Real defects found & fixed: never-applied V6..V11 migrations (MariaDB-only
syntax; local DB was at V5 — corrected + applied, data preserved); null job
control signals on create; pause/stop clobbered by stale-copy full-row UPDATE
(rewritten to narrow disjoint updates); complete() ordering for AC-113;
TextLengthUtil blank=0.

Phase 4 Gate:
PASSED (2026-08-22) — TASK-133..139, full mvn test 40/40; AC-106 real-LLM
PASS after one honest FAIL→fix→re-verify cycle (completed-beats list injected
into replan prompt). Fixed en route: stale-total STEP completion, resolver
counting superseded rows as pending, runStep exception convergence.

Phase 5 Gate:
PASSED (2026-08-22) — TASK-140..149, full mvn test 46/46.
ChapterRevision model (V12 + 44-chapter backfill), manual edit, approve,
regenerate, revision history API+UI, memory STALE + candidate reverse-lookup
invalidation. AC-107 + AC-108 real-LLM PASS (iron_refs=0 after author edit).

Phase 6 Gate:
PASSED (2026-08-22) — TASK-150..156, full mvn test 53/53.
targetChapterCount CRUD/PATCH/UI; Arc V13 + API + ArcPanel UI;
LongFormPosition contract both ends; proportional pace guard prompt.
AC-114 real-LLM PASS: target=600/current=5/arc=1-60 with a DELIBERATELY
endgame-seeking direction produced an arc-scoped plan, 0 endgame patterns
(.agent/evidence/ac114_plan.json).

Phase 7 Gate:
PASSED (2026-08-22) — TASK-157..165, full mvn test 58/58.
Memory v2 (V14 importance/scope/active), frozen type enum with normalize
mapping, extractor five-question prompt, safe processing guards (unknown type
REVIEW; item:* importance<4 REVIEW), dedup v1 (AC-115), inventory multi-item
slots (AC-116), writer memory selection (excludes transient/importance<=2).
AC-105 real-LLM PASS: bread detail classified imp=1/IGNORE — never reaches
the writer context.

Phase 8 Gate:
PASSED (2026-08-22) — TASK-166..171, full mvn test 59/59.
writingStyle wired to Writer contract+prompt; polish contract/prompt/workflow
(POST /ai/polish-chapter; AI_POLISH revision → STALE → re-extract);
polish UI. AC-109 real-LLM PASS: 7/7 structured fact-preservation checks.

Environment change note (2026-08-22):
Maven 3.9.16 + JDK17 now available in this environment (previously absent).
mvn test fully operational against the local MySQL story_ai database.

Known Blocker:
NONE — BLOCKER-1 resolved on 2026-08-22: owner switched the model to
qwen3.7-plus; AC-103 re-run under frozen conditions produced
3197/2788/2818/3795/3227 chars = 4/5 in band → PASS (ac103_rerun.json);
manual inspection confirmed rich sensory prose, no padding. Original
qwen3-8b FAIL records retained as capability-difference evidence.

Next Safe Action:
None required for v0.1.1 — it is ACCEPTED. Future work belongs to a new
frozen version (candidates already recorded in the Deferred Backlog).
```

# 21. Release Hardening Authority (Current)

`V0.1.1_RELEASE_HARDENING.md` supersedes the earlier release verdict. The
repository is currently:

> **Feature Complete, Release Hardening Required**

Release verdict:

`NOT ACCEPTED — RH-05 through RH-10 remain; RH-11 is gated`

Hardening progress:

```text
RH-01 DONE — AC-H01 PASSED (engineering)
RH-02 DONE — AC-H02 PASSED (engineering)
RH-03 DONE — AC-H03 PASSED (engineering)
RH-04 DONE — AC-H04/H05/H06 PASSED (engineering)
RH-05 NEXT
RH-06 TODO
RH-07 TODO
RH-08 TODO
RH-09 TODO
RH-10 TODO — final qwen3.7-plus semantic suite
RH-11 BLOCKED BY RH-05~RH-10
```

RH-01 verification details:

```text
Regression before fix:
ChapterRevisionIntegrationTest — 1 failure
manualEditRefreshesSummaryMemoryAndRecentWriterContext
(old chapter.summary still contained the deleted iron-sword fact)

Engineering Verification:
PASSED
- ChapterRevisionIntegrationTest: 8/8
- ai-service pytest: 8/8

Real-LLM Semantic Verification:
NOT_REQUIRED
```

RH-02 verification details:

```text
Regression before fix:
MemoryProvenanceIntegrationTest — 2 failures
- normalized item:铁剑 remained after revising its source chapter
- revising Chapter 1 deleted Chapter 2's newer location/relationship values

Engineering Verification:
PASSED
- MemoryProvenanceIntegrationTest: 2/2
- Related Memory + Revision integration tests: 16/16
- V15 additive migration applied to the existing local schema without data loss

Real-LLM Semantic Verification:
NOT_REQUIRED
```

RH-03 verification details:

```text
Regression before fix:
ReplanRemainingIntegrationTest — 1 failure
expected V2 orders [4,5,6], actual [10,11,12]

Engineering Verification:
PASSED
- ReplanRemainingIntegrationTest: 7/7
- AC-H03 generated Chapters 1..6 and captured Writer chapterOrder 1..6
- V1 orders 4..9 remain queryable as SUPERSEDED history

Real-LLM Semantic Verification:
NOT_REQUIRED (AC-106 final semantic rerun is RH-10)
```

RH-04 verification details:

```text
Regression before fix:
Frontend RH-04 suite — 4 failures / 5 tests
- no PENDING/RUNNING polling (progress remained 0/3)
- no pauseGeneration / stopGeneration API functions
- no Pause / Stop buttons

Engineering Verification:
PASSED
- Frontend Vitest: 5/5
- Frontend production build: PASSED
- Backend Pause/Stop checkpoint regressions: 2/2

Real-LLM Semantic Verification:
NOT_REQUIRED
```

Core principles:

> **AI proposes. Java decides. MySQL remembers.**

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

> **Mock proves plumbing. Real LLM proves AI behavior.**


