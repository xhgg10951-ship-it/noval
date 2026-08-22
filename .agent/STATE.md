# .agent/STATE.md

> Project: **AI Story Co-Author**
>
> Active Version: **v0.1.1**
>
> State Type: **INITIAL IMPLEMENTATION STATE**
>
> Updated: **2026-08-22**

# 1. Current Project State

Current Version:

`v0.1.1`

Requirement Status:

`FROZEN`

Frozen Requirement Document:

`V0.1.1_IMPROVEMENT_PLAN.md`

Current Implementation Plan:

`.agent/TASKS.md`

Current Phase:

`Phase 0 — Correct Evidence Base`

Current Task:

`TASK-103 — Capture Current AI Request Payloads`

Task Status:

`TODO`

Task Evidence:

TASK-101/102 DONE — regression baseline at `.agent/EVIDENCE_v0.1_REGRESSION.md`;
status corrections recorded in §2.1.

Engineering Verification:
NOT_RUN — TASK-103 adds payload observability

Real-LLM Semantic Verification:
NOT_REQUIRED

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

> real-LLM semantic quality and long-form authoring behavior are not accepted yet.

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

`NONE KNOWN`

Potential external requirement for later semantic tests:

```text
A configured real LLM API credential
```

This is NOT a Phase 0 blocker.

It becomes a blocker only when a task explicitly requires Real-LLM Semantic Verification and no usable real provider credential exists.

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
Phase 0
TASK-101
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
NOT STARTED

Current Phase:
Phase 0 — Correct Evidence Base

Current Task:
TASK-103 — Capture Current AI Request Payloads

Current Task Status:
TODO

Known Blocker:
NONE

Next Safe Action:
Add minimal debug observability for PlanStageRequest / GenerateChapterRequest / ExtractMemoryRequest (no API key logging); enable Prompt/Payload review before Phase 1.

Engineering Verification:
NOT_RUN

Real-LLM Semantic Verification:
NOT_REQUIRED
```

Core principles:

> **AI proposes. Java decides. MySQL remembers.**

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

> **Mock proves plumbing. Real LLM proves AI behavior.**
