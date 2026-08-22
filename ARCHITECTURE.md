# ARCHITECTURE.md

# AI Story Co-Author v0.1.1 Architecture

## Core

> **AI proposes. Java decides. MySQL remembers.**

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

```text
Vue 3 SPA
  ↓ HTTP/JSON
Spring Boot
  ├── Business State / Context Assembly / Workflow
  ├── MyBatis → MySQL
  └── HTTP/JSON → Python AI Service
                     ├── Planner
                     ├── Writer
                     ├── Memory Extractor
                     ├── Polish
                     └── Query/Suggestions
```

Vue 不直连 Python；Python 不直接修改业务 DB。

## Domain Model

```text
Story
├── StoryConstraint
├── LongFormSettings
├── Arc
│   └── Stage
│       ├── PlanVersion / ChapterSpec
│       └── Chapter → ChapterRevision
├── CurrentState
├── RelationshipState
├── StoryMemory
└── MemoryCandidate

Stage → GenerationJob
```

不增加 Volume。

## Story / Arc / Stage

Story 新增 `defaultTargetCharacters / targetChapterCount / writingStyle`。Arc 是数十章级目标。Stage 生命周期 `PLANNING → ACTIVE → COMPLETED` 或 ABANDONED。

## ChapterSpec / Plan Version

ChapterSpec：`goal / expectedProgress / targetCharacters / mustAdvance / mustNotDo / storyBeats / endingIntent / planVersion / active / status`。Replan Remaining 不物理删除已生成 Chapter 对应 Spec；旧未来计划 superseded。

## Chapter / Revision

Chapter 保持稳定 ID，正文版本放 ChapterRevision。Chapter 至少记录 `currentRevisionId / status / memoryExtractionStatus`。Revision Source：AI_GENERATED / MANUAL_EDIT / AI_POLISH / AI_REWRITE。

Memory Extraction Status：PENDING / COMPLETED / FAILED / STALE。

## Single Chapter Recovery Boundary

```text
Resolve Next Safe Action
→ Generate if missing
→ Persist Chapter + Revision
→ Memory PENDING
→ Extract
→ Apply safe candidates
→ Memory COMPLETED
→ Checkpoint
```

LLM 调用不放在长事务中。Recovery 不只依赖 `currentPlanIndex`：Chapter missing → Generate；Chapter exists + memory PENDING/FAILED/STALE → Extract；checkpoint complete → Next Plan。

## GenerationJob v2

状态可覆盖 `PENDING / RUNNING / PAUSE_REQUESTED / PAUSED / COMPLETED / FAILED / STOPPED`。Continuous 改成应用内后台 executor：POST start 持久化 Job 并快速返回；后台单元执行；前端 polling。允许 TaskExecutor/@Async/ExecutorService，不引入 MQ。

Pause 在当前安全 checkpoint 生效；Stop 保留已完成 Chapter。全部 active plan 完成且 memory checkpoint 正常后，Job 与 Stage 都 COMPLETED。

## Context Assembly

Spring Boot owns context assembly。

Planner：Core Idea、Constraints、Target Chapter Count、Current Chapter Number、Current Arc、Completed Stage Summaries、Current State、Relationship、Selected Memory、Recent Summaries、Continuation Anchor、Stage Direction。

Writer：Constraints、Long-form Position、Arc、Stage、ChapterSpec、Current State、Relationship、Selected Memory、2–3 Recent Summaries、Last Ending。

Writer Priority：Hard Constraints > Long-form Position > Arc > Stage > ChapterSpec > State > Selected Memory > Recent Context。

## Memory v2

Types：CURRENT_STATE / RELATIONSHIP / PLOT_FACT / PLOT_THREAD / FORESHADOWING / WORLD_RULE / TRANSIENT_DETAIL。Importance 1–5；Scope CHAPTER/STAGE/ARC/STORY；保留 sourceChapterId/evidence。未知 type 不得 catch-all 自动长期保存。

不把全部 StoryMemory 发给 Writer。Current State、相关 Relationship、importance=5 常驻；当前目标相关 memory 可选；TRANSIENT_DETAIL 和 importance<=2 默认排除。

Dedup v1 不用 Embedding。Inventory 使用 `item:<normalized-name>` 形式支持多物品。

## Revision Memory Reconciliation

当前 Revision 变化 → memory STALE → 失效该 Chapter 派生旧记忆 → re-extract → safe apply → COMPLETED。v0.1.1 不做复杂 semantic merge。

## AI Endpoints

保持/扩展：`/ai/plan-stage /ai/replan-stage /ai/generate-chapter /ai/extract-memory /ai/polish-chapter /ai/suggest-directions /ai/story-query`。Structured input/output。

## Non-goals

不引入 RAG/Vector、Redis/MQ、Event Sourcing、全面 DDD/Hexagonal、microservices、Knowledge Graph、autonomous recursive agents。Polling 足够时不加 SSE/WebSocket。
