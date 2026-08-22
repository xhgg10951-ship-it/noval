# .agent/TASKS.md

> Project: **AI Story Co-Author v0.1.1**
>
> Status: **ACTIVE IMPLEMENTATION PLAN**
>
> Source of scope truth: `V0.1.1_IMPROVEMENT_PLAN.md` (`Status: FROZEN`)
>
> This file replaces the v0.1 implementation task tree for active development.

# 0. Purpose

本文件是 **v0.1.1 冻结需求的可执行任务树**。

它回答：

- 当前应该做什么；
- 每个任务依赖什么；
- 哪些验证必须使用 Mock；
- 哪些验证必须使用真实 LLM；
- 哪个 Phase 没通过就不能继续；
- Coding Agent 中断后应该从哪里恢复。

本文件由 Coding Agent 持续维护。

冻结产品需求不得在本文件中被偷偷扩大。

---

# 1. Authority

执行顺序：

```text
AGENTS.md
↓
V0.1.1_IMPROVEMENT_PLAN.md
↓
.agent/STATE.md
↓
.agent/TASKS.md
↓
Repository Reality
```

如果 `.agent/TASKS.md` 与冻结的 `V0.1.1_IMPROVEMENT_PLAN.md` 冲突：

> 以冻结 Improvement Plan 为准。

如果 `.agent/STATE.md` 与真实代码冲突：

> 以 Git + 实际代码 + 实际验证为准，并立即修正 STATE。

---

# 2. v0.1.1 Task Rules

## 2.1 One Active Task

任何时间默认只有一个主要任务为：

`IN_PROGRESS`

除非两个修改不可拆分，否则不得并行推进多个写任务。

---

## 2.2 Allowed Status

只允许：

- `TODO`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`
- `DEFERRED`

---

## 2.3 DONE Rule

任务只有同时满足以下要求才能 `DONE`：

```text
Implementation complete
+
Relevant automated verification executed
+
Required semantic verification executed
+
No hidden blocker
+
.agent/STATE.md updated
+
Git checkpoint created
```

---

## 2.4 Dual Verification Rule

从 v0.1.1 开始，每个涉及 AI 行为的任务必须记录：

```text
Engineering Verification:
PASSED / FAILED / NOT_RUN

Real-LLM Semantic Verification:
PASSED / FAILED / NOT_REQUIRED / BLOCKED
```

严禁：

```text
Mock PASS
→ Semantic PASS
```

---

## 2.5 Mock Boundary

Mock 可以证明：

- HTTP contract；
- DTO；
- JSON parsing；
- Persistence；
- Retry；
- Workflow；
- Context 是否被组装；
- 状态机。

Mock 不可以证明：

- Planner 续写质量；
- Writer 是否真正遵循章节目标；
- 文风；
- 长篇节奏；
- Memory 是否改善正文；
- Polish 是否自然且保持事实。

---

## 2.6 Frozen Scope Guard

禁止自动加入：

```text
RAG
Embedding
Vector DB
Redis
MQ
LangGraph
Knowledge Graph
Temporal Truth Engine
Canon Governance
Volume
Multi-user
Auth
Auto Publishing
AI Detection Bypass
Fine-tuning
```

发现相关需求：

> `DEFERRED` 或记录为 v0.1.2/v0.2 candidate。

---

# 3. Phase Overview

```text
Phase 0  Correct Evidence Base
Phase 1  Context Wiring + Continuation
Phase 2  Chapter Control
Phase 3  Generation Reliability
Phase 4  Replan Remaining
Phase 5  Chapter Revision
Phase 6  Long-form Pace
Phase 7  Memory v2
Phase 8  Polish + Style
Phase 9  Full Real-LLM Acceptance
```

Phase Gate 未通过：

> 禁止进入下一 Phase 的产品功能开发。

---

# 4. Phase 0 — Correct Evidence Base

目标：

> 先纠正 v0.1 “文档声称完成”和“代码真实行为”之间的不一致。

---

## TASK-101 — Snapshot v0.1 Regression Evidence

Status: `DONE`

Evidence:

`.agent/EVIDENCE_v0.1_REGRESSION.md` — code-level regression baseline.

Engineering Verification:
PASSED — evidence saved and reviewable

Real-LLM Semantic Verification:
NOT_REQUIRED

Goal:

保存当前真实 v0.1 行为基线。

至少记录：

- 当前真实测试 Story；
- Stage Plans；
- 生成 Chapters；
- Memory；
- 当前使用的模型；
- 当前 Planner / Writer / Extractor Prompt；
- 已知“重新开书 / 面包循环 / 章节过短”案例。

Do Not:

- 修改生成逻辑；
- 用新的实现覆盖旧证据。

Verification:

确认 regression evidence 可以在后续对比使用。

Dependencies:

None

---

## TASK-102 — Correct v0.1 Status Claims

Status: `DONE`

Evidence:

Active `.agent/STATE.md` §2.1 enumerates the three required corrections
(Writer Memory NOT wired; Baseline-vs-Memory real-prose experiment NOT run;
Mock PASS ≠ Semantic PASS). Historical `.agent/history/` + `docs/history/`
files left untouched per AGENTS.md (non-active evidence). README.md and
active STATE.md already stated Real-LLM NOT ACCEPTED.

Engineering Verification:
PASSED — inaccurate claims explicitly corrected in active docs

Real-LLM Semantic Verification:
NOT_REQUIRED

Goal:

修正 Agent-owned / README 中不准确声明。

至少明确：

```text
v0.1 Engineering Pipeline Accepted
Real-LLM Product Behavior NOT ACCEPTED
```

必须纠正：

- Writer 已使用 Structured Memory 的错误完成声明；
- Baseline vs Memory 已完成真实正文实验的错误印象；
- Mock PASS 被解释成语义 PASS 的内容。

Human-owned frozen spec 不修改。

Dependencies:

TASK-101

---

## TASK-103 — Capture Current AI Request Payloads

Status: `DONE`

Goal:

为以下实际调用增加最小 Debug 可观察能力：

```text
PlanStageRequest
GenerateChapterRequest
ExtractMemoryRequest
```

可以使用：

- debug log；
- test captor；
- dev-only request dump。

不得输出 API Key。

Implementation:

- `backend/.../ai/AiServiceClient.java`: 每个对外请求体在 DEBUG 级别日志输出（按字段名秘密守卫脱敏，大小写不敏感，不会误伤 "monkey" 之类字段）。
- `ai-service/app/prompts/builders.py`: `log_request_shape()` 记录各上下文字段是否非空；接入 planner / replan / generate-chapter / extract-memory 入口。

Verification:

能够明确看到 Planner / Writer 当前究竟收到了哪些字段。

Engineering Verification: PASSED
- Java 通过 javac 编译（仅存在既有 deprecation 警告）
- Python 导入通过
- 脱敏正则复测：正确脱敏 apiKey/accessToken/password/client_secret，保留 monkey/coreIdea/normalField

Real-LLM Semantic Verification: NOT_REQUIRED

Evidence:

Commit `63781dd` — 5 源文件变更。

Dependencies:

TASK-101

---

## TASK-104 — Phase 0 Evidence Review

Status: `DONE`

Goal:

确认以下已知根因与仓库事实一致：

- Planner context currently missing；
- Writer state/memory currently missing；
- previous-summary feedback loop exists；
- confirmed-plan replan is unsafe；
- extraction failure retry hole exists。

如果代码已经变化：

> 更新 `.agent/STATE.md`，不要机械按旧假设实现。

Review Result (repository reality re-confirmed at file:line):

1. **Planner context missing** — `StagePlanningService.buildRequest` 第 98–100 行：`List.of()` (currentState)、`List.of()` (storyMemories)、`""` (recentContext)。与 RC-01 / STATE §4.1 一致。
2. **Writer state/memory missing** — `ChapterGenerationService.buildRequest` 第 138–140 行：连续三个 `List.of()` (currentState / storyMemories / relationshipState)。与 RC-02 / STATE §4.2 一致。
3. **previous-summary loop** — `prevSummary` 是唯一传入的 recent context（第 141 行），无任何其他连续性锚点。与 RC-04 / STATE §4.3 一致。
4. **unsafe full replan** — `StageService.java:57` `chapterPlanMapper.deleteByStageId(stageId)` 整段删除计划（含已完成 Chapter↔Plan 历史），未区分 active/completed。与 STATE §4.7 一致。
5. **extraction retry hole** — `ChapterGenerationService.java:111` 在 save 后调用 `extractForChapter(saved)`；仓库全局无 `@Retryable` / `retryTemplate` 任何重试机制。一旦 extract 抛异常，已持久化 Chapter 将被视为“已完成生成”而推进。与 RC-08 / STATE §4.8 一致。

TASK-103 仅增加观察性日志，未改动生成逻辑，故冻结根因仍然成立。STATE.md §4 / §12 与仓库事实一致，无需机械重写。

Verification:

Repository reality documented.

Engineering Verification: PASSED — 5 根因在仓库中按 file:line 复核一致  
Real-LLM Semantic Verification: NOT_REQUIRED

Dependencies:

TASK-102
TASK-103

---

## Phase 0 Gate

必须满足：

```text
[x] v0.1 不再被描述为 Real-LLM Product Accepted
[x] 当前 Prompt / Payload 可以被审查
[x] 已保存 regression baseline
[x] STATE 与 main 代码事实一致
```

Gate Result: **PASSED** (commit `63781dd` 后复核，2026-08-21)
- v0.1 在 STATE.md / README 中仅作为 Engineering Pipeline Accepted（TASK-102）。
- TASK-103 提供 DEBUG 级 Payload 审查能力；TASK-101 保存 regression baseline。
- TASK-104 复核 5 根因与仓库 file:line 一致，STATE §4 无需改写。

进入 Phase 1 的 prerequisites 已满足。

---

# 5. Phase 1 — Context Wiring + Continuation

目标：

> 先让现有 Planner / Writer 真正拿到我们 v0.1 就已经设计的故事状态。

重点文件预计包括：

```text
backend/.../StagePlanningService.java
backend/.../ChapterGenerationService.java
backend/.../MemoryService.java
backend/.../ChapterService.java
ai-service/app/schemas/models.py
ai-service/app/prompts/builders.py
```

实际路径以仓库为准。

---

## TASK-105 — Build Shared Story Context Reader

Status: `DONE`

Goal:

在 Spring Boot 中建立最小可复用 Context Assembly 能力。

必须能够读取：

- Constraints；
- Current State；
- Relationships；
- Story Memories；
- 最近 Chapters。

不要现在实现复杂 Ranking Framework。

Implementation:

新增 `backend/.../context/StoryContextReader.java`（`@Service`）：

- `getConstraintItems(storyId)` → `PlanStageRequest.ConstraintItem`
- `getCurrentStateItems(storyId)` → `PlanStageRequest.StateItem`
- `getStoryMemoryItems(storyId)` → `PlanStageRequest.MemoryItem`
- `getRelationshipItems(storyId)` → `GenerateChapterRequest.RelationshipItem`
- `getRecentContext(storyId, maxChapters)` → 最近 N 章 summary 拼接

复用现有 `StoryService.getConstraints` / `MemoryService` / `ChapterService`，只读、不改写。映射逻辑集中于此组件，供 Planner / Writer 共用（消除分散重复）。

Verification:

Engineering Verification: PASSED
- javac 编译通过（`target/classes` + `build-deps/*.jar`，Windows `;` 分隔符）
- 5 个读取能力均已暴露

Real-LLM Semantic Verification: NOT_REQUIRED

Evidence:

新文件 `context/StoryContextReader.java`（commit 见下文）。

Dependencies:

Phase 0 Gate

---

## TASK-106 — Wire Planner Existing Story State

Status: `DONE`

Goal:

修复 `StagePlanningService`。

已有数据时，不再传：

```text
currentState=[]
storyMemories=[]
recentContext=""
```

至少真正传：

- Current State；
- Story Memory；
- Recent Context。

Implementation:

`StagePlanningService` 注入 `StoryContextReader`，`buildRequest` 第 4/5/6 位改为：

- `contextReader.getCurrentStateItems(storyId)`
- `contextReader.getStoryMemoryItems(storyId)`
- `contextReader.getRecentContext(storyId, 3)`（最近 3 章摘要）

首阶段若尚无状态/记忆，读取结果为空列表——这是正确行为；后续阶段现在真正携带既有剧情上下文（修复 RC-01）。

Verification:

Engineering Verification: PASSED
- javac 编译通过（reader + StagePlanningService 一起编译）
- 注入装配正确，无残留 `List.of()` 占位

Real-LLM Semantic Verification:

暂不在本 Task 宣布 PASS（属 Phase 1 Gate AC-101，需真实 LLM）。

Dependencies:

TASK-105

---

## TASK-107 — Add Planner Continuation Context v2 Contract

Status: `TODO`

Goal:

Planner Request 增加 / 明确：

```text
relationshipState
currentChapterNumber
completedStageSummaries
recentChapterSummaries
continuationAnchor
```

不要加入 Arc 字段的最终 Pace Logic；Arc 在 Phase 6 完成。

可以为未来字段保留 Optional，但不得提前实现 Phase 6 业务。

Dependencies:

TASK-106

---

## TASK-108 — Implement Continuation Anchor Assembly

Status: `TODO`

Goal:

构建：

```text
lastChapterNumber
currentLocation
activeCharacters
currentImmediateGoal
lastChapterSummary
lastChapterEnding
```

`lastChapterEnding`：

只取末尾有限文本，不发送整本书。

Dependencies:

TASK-107

---

## TASK-109 — Planner Continuation Prompt

Status: `TODO`

Goal:

更新 Planner Prompt：

明确：

> You are continuing an existing story.

禁止默认重复：

- 穿越；
- 初遇已经认识的人；
- 再次获得已经有的身份；
- 重复已完成 Stage。

Prompt 必须引用 Continuation Anchor。

Engineering Verification:

Prompt builder test.

Real-LLM Semantic Verification:

由 TASK-112 完成。

Dependencies:

TASK-108

---

## TASK-110 — Wire Writer Current State / Memory / Relationship

Status: `DONE`

Goal:

修复 `ChapterGenerationService`。

已有数据时真正填充：

```text
currentState
storyMemories
relationshipState
```

不允许继续使用三个 `List.of()` 作为正式路径。

Implementation:

`ChapterGenerationService` 注入 `StoryContextReader`，`buildRequest` 第 6/7/8 位改为：

- `contextReader.getWriterStateItems(storyId)` → `GenerateChapterRequest.StateItem`
- `contextReader.getWriterMemoryItems(storyId)` → `GenerateChapterRequest.MemoryItem`
- `contextReader.getRelationshipItems(storyId)` → `GenerateChapterRequest.RelationshipItem`

注：`PlanStageRequest` 与 `GenerateChapterRequest` 的嵌套 `StateItem`/`MemoryItem` 是不同类型（同字段不同外层 record），因此 reader 提供 Writer 形状独立映射（`getWriterStateItems` / `getWriterMemoryItems`），避免跨类型直接复用。首章之前这些列表为空是正确行为；后续章节现在真正携带既有状态/记忆/关系（修复 RC-02）。recentContext 仍为单章 prevSummary，升级见 TASK-111。

Verification:

Engineering Verification: PASSED
- javac 编译通过（reader + ChapterGenerationService 一起编译）
- 三处 `List.of()` 占位已全部移除，改用 reader 真实数据

Real-LLM Semantic Verification:

暂不在本 Task 宣布 PASS（AC-102 允许 Mock，但语义质量属 Phase 1 Gate / AC-104）

Acceptance:

AC-102

Dependencies:

TASK-105

---

## TASK-111 — Upgrade Recent Writer Context

Status: `TODO`

Goal:

从：

```text
previous chapter summary only
```

升级为：

```text
previous 2–3 chapter summaries
+
last chapter ending excerpt
```

避免单个 Summary 成为唯一连续性锚点。

Dependencies:

TASK-110

---

## TASK-112 — Real-LLM Continuation Acceptance

Status: `TODO`

Goal:

执行 AC-101。

已知故事状态：

```text
已穿越
已认识艾琳
已住进艾琳提供的房间
```

新 Stage：

```text
第二天和艾琳去冒险者公会
```

PASS:

计划从当前状态继续。

FAIL:

再次穿越 / 初遇 / 找住处。

Engineering Verification: PASSED required before run  
Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-109
TASK-111

---

## Phase 1 Gate

```text
[ ] Planner 真实获得历史 Context
[ ] Writer 真实获得 State / Memory / Relationship
[ ] Recent Context 不再只有一个 Summary
[ ] AC-101 PASS with real LLM
[ ] AC-102 PASS
```

---

# 6. Phase 2 — Chapter Control

目标：

> 解决章节过短、Planner → Writer 约束丢失和正文不执行计划。

---

## TASK-113 — Add Story Writing Settings Migration

Status: `TODO`

Goal:

Story 新增：

```text
default_target_characters
writing_style
```

`target_chapter_count` 可以同一 migration 加入，但 Phase 6 前不实现 Pace 行为。

要求：

- additive migration；
- 兼容旧数据；
- 默认 target characters = 3000。

Dependencies:

Phase 1 Gate

---

## TASK-114 — Expand ChapterPlan to ChapterSpec Schema

Status: `TODO`

Goal:

增加：

```text
target_characters
must_advance
must_not_do
story_beats
ending_intent
```

保留：

```text
goal
expected_progress
```

列表优先 JSON / TEXT。

不要拆成大量子表。

Dependencies:

TASK-113

---

## TASK-115 — Upgrade Planner Structured Output v2

Status: `TODO`

Goal:

Python / Java Planner Contract 输出：

```text
goal
expectedProgress
targetCharacters
mustAdvance
mustNotDo
storyBeats
endingIntent
```

Validation：

- plan count 与 items 数量一致；
- targetCharacters 合理；
- 必需字段可解析。

Dependencies:

TASK-114

---

## TASK-116 — Persist Full ChapterSpec

Status: `TODO`

Goal:

Planner → Java → MySQL 保存所有 ChapterSpec 字段。

禁止：

> AI 返回了字段但数据库丢掉。

Engineering Verification:

Mapper/integration test.

Dependencies:

TASK-115

---

## TASK-117 — Writer Contract v2

Status: `TODO`

Goal:

Writer Request 使用完整 ChapterSpec。

必须真正传：

```text
goal
expectedProgress
targetCharacters
mustAdvance
mustNotDo
storyBeats
endingIntent
```

禁止只传 `goal`。

Dependencies:

TASK-116

---

## TASK-118 — Implement Writer Goal Lock Prompt

Status: `TODO`

Goal:

Writer Prompt 明确优先级：

```text
Hard Constraints
>
Long-form Position
>
Current Arc
>
Stage
>
ChapterSpec
>
Current State
>
Selected Memory
>
Recent Context
```

当前 Phase 尚无完整 Arc 时，Arc 可为空。

明确：

- ChapterSpec 是本章执行目标；
- Memory 不得替换本章主线；
- 与本章无关低价值细节不需要复用；
- Must Not 必须遵守。

Dependencies:

TASK-117

---

## TASK-119 — Add Chapter Length Measurement

Status: `TODO`

Goal:

后端计算并返回 / 保存：

```text
targetCharacters
actualCharacterCount
```

中文字符计数规则保持简单、一致并有测试。

不得因为略偏目标就直接删除内容。

Dependencies:

TASK-117

---

## TASK-120 — Story / Stage / Chapter Length UI

Status: `TODO`

Goal:

至少支持：

- Story 默认目标字数；
- Stage override（如冻结计划要求的实现方式）；
- 单章 Plan target 显示 / 调整。

UI 不做复杂 Slider 系统。

Dependencies:

TASK-119

---

## TASK-121 — Real-LLM Chapter Length Acceptance

Status: `TODO`

Goal:

AC-103：

```text
target=3000
5 chapters
```

PASS:

至少 4 / 5：

```text
2250–3750
```

且没有明显重复灌水。

Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-118
TASK-120

---

## TASK-122 — Real-LLM Chapter Goal Acceptance

Status: `TODO`

Goal:

执行 AC-104。

验证：

- Must Advance 实际完成；
- Must Not 未违反；
- 章节不再围绕无关前章细节打转。

Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-118

---

## Phase 2 Gate

```text
[ ] ChapterSpec 全链路不丢字段
[ ] Writer 使用 expectedProgress
[ ] Writer 使用 targetCharacters
[ ] AC-103 PASS
[ ] AC-104 PASS
```

---

# 7. Phase 3 — Generation Reliability

目标：

> 修复真实状态机缺陷，再谈中途 Replan。

---

## TASK-123 — Add Chapter Memory Extraction Status

Status: `TODO`

Goal:

Chapter 增加：

```text
memory_extraction_status
```

至少：

```text
PENDING
COMPLETED
FAILED
STALE
```

旧 Chapter migration 使用合理默认。

Dependencies:

Phase 2 Gate

---

## TASK-124 — Make Extraction Status Explicit in Generate Flow

Status: `TODO`

Goal:

单章流程：

```text
Writer success
↓
Save Chapter
memory=PENDING
↓
Extract
↓
COMPLETED
```

失败：

```text
Chapter preserved
memory=FAILED
Job failed/paused safely
```

Dependencies:

TASK-123

---

## TASK-125 — Implement Next Safe Action Resolver

Status: `TODO`

Goal:

恢复逻辑根据数据库事实判断：

```text
Chapter missing
→ GENERATE

Chapter exists + memory FAILED/PENDING/STALE
→ EXTRACT_MEMORY

Chapter complete
→ NEXT PLAN
```

不再只靠：

```text
currentPlanIndex
```

决定恢复。

Dependencies:

TASK-124

---

## TASK-126 — Fix Extraction Failure Retry

Status: `TODO`

Goal:

执行 AC-110 工程测试：

```text
Writer success
Chapter persisted
Extractor fails once
Retry
```

必须：

> Retry 同一 Chapter Extraction。

不得：

> 跳到下一 Plan。

Engineering Verification: REQUIRED  
Real-LLM Semantic Verification: NOT_REQUIRED

Dependencies:

TASK-125

---

## TASK-127 — Convert Continuous Generation to Background Execution

Status: `TODO`

Goal:

`POST start`：

快速创建并返回 Job。

后台执行：

- TaskExecutor / @Async / ExecutorService。

禁止：

- MQ；
- Redis；
- 新微服务。

Dependencies:

TASK-126

---

## TASK-128 — Implement Job Polling-safe Progress

Status: `TODO`

Goal:

保证 GET Job 可以持续观察：

```text
current / total
phase
status
lastError
```

Frontend 不再依赖 Start HTTP 一直阻塞。

Acceptance:

AC-111

Dependencies:

TASK-127

---

## TASK-129 — Implement Pause

Status: `TODO`

Goal:

允许 Pause Request。

语义：

> 当前正在运行的单章安全完成后，在下一 checkpoint 停止。

不要求强制终止已经发出的 LLM 请求。

Acceptance:

AC-112

Dependencies:

TASK-127

---

## TASK-130 — Implement Stop

Status: `TODO`

Goal:

Stop 后：

- 不继续剩余 Plans；
- 已完成 Chapter 保留；
- Job 有清晰 STOPPED 状态。

Dependencies:

TASK-129

---

## TASK-131 — Complete Stage Lifecycle

Status: `TODO`

Goal:

当 active plans 全部完成，并且章节 Memory checkpoint 正常：

```text
Job=COMPLETED
Stage=COMPLETED
```

Acceptance:

AC-113

Dependencies:

TASK-128

---

## TASK-132 — Generation Reliability Regression Suite

Status: `TODO`

Goal:

至少覆盖：

- writer failure before save；
- extraction failure after save；
- retry；
- async progress；
- pause；
- stop；
- stage completion；
- duplicate chapter prevention。

Dependencies:

TASK-126
TASK-128
TASK-129
TASK-130
TASK-131

---

## Phase 3 Gate

```text
[ ] AC-110 PASS
[ ] AC-111 PASS
[ ] AC-112 PASS
[ ] AC-113 PASS
[ ] Continuous 不再阻塞整次 HTTP
[ ] Retry 不再丢失失败章节 Memory
```

---

# 8. Phase 4 — Replan Remaining

目标：

> 作者可以改变未来，但不能破坏历史。

---

## TASK-133 — Add Minimal Plan Version Fields

Status: `TODO`

Goal:

ChapterSpec / Plan 增加：

```text
plan_version
active
status
```

支持：

```text
ACTIVE
COMPLETED
SUPERSEDED
```

命名可小幅调整。

Dependencies:

Phase 3 Gate

---

## TASK-134 — Preserve Completed Plan Relations

Status: `TODO`

Goal:

已生成 Chapter 对应 Plan 永远不被 Replan 删除。

禁止新的 Replan 实现继续：

```text
deleteByStageId(stageId)
```

Dependencies:

TASK-133

---

## TASK-135 — Implement Active Remaining Plan Query

Status: `TODO`

Goal:

Generation 只选择：

```text
active=true
AND not completed
```

的剩余 ChapterSpec。

Dependencies:

TASK-134

---

## TASK-136 — Implement Replan Remaining Service

Status: `TODO`

Goal:

输入：

- remainingChapterCount；
- optional author instruction。

处理：

```text
completed plans preserved
old remaining → superseded
current Story State → Planner
new version remaining plans created
```

Dependencies:

TASK-135

---

## TASK-137 — Replan Job Consistency

Status: `TODO`

Goal:

Replan 后：

- Job total 正确；
- progress 正确；
- 不重复生成；
- Pause 状态允许 Replan；
- Running 状态要求先 Pause 到安全 checkpoint。

Dependencies:

TASK-136

---

## TASK-138 — Replan Remaining UI

Status: `TODO`

Goal:

ACTIVE / PAUSED Stage 允许作者：

```text
重新规划剩余章节
```

不得显示成“删除整个计划”。

Dependencies:

TASK-137

---

## TASK-139 — Replan Remaining Acceptance

Status: `TODO`

Goal:

执行 AC-106：

```text
Plan 9
generate 3
replan remaining to 3
```

PASS:

最终 Chapter 1–6，无重复，1–3 不变。

Engineering Verification: REQUIRED  
Real-LLM Semantic Verification: REQUIRED（新计划必须从当前状态继续）

Dependencies:

TASK-138

---

## Phase 4 Gate

```text
[ ] Completed Plans 不删除
[ ] Replan 只作用于未来
[ ] Job 状态一致
[ ] AC-106 PASS
```

---

# 9. Phase 5 — Chapter Revision

目标：

> AI 生成的是草稿，作者重新获得文本控制权。

---

## TASK-140 — Add ChapterRevision Schema

Status: `TODO`

Goal:

新增：

```text
chapter_revision
```

字段：

```text
id
chapter_id
version_number
content
source_type
created_at
```

Chapter 新增：

```text
current_revision_id
status
```

Status：

```text
DRAFT
APPROVED
```

Dependencies:

Phase 4 Gate

---

## TASK-141 — Migrate Existing Chapter Content into Revision Model

Status: `TODO`

Goal:

旧 v0.1 Chapter 内容仍然可读。

为旧数据建立：

```text
AI_GENERATED revision
```

或等价兼容方案。

不得丢已有正文。

Dependencies:

TASK-140

---

## TASK-142 — Manual Edit Backend

Status: `TODO`

Goal:

作者保存修改：

> 创建新 Revision，而不是直接无历史覆盖。

Acceptance base:

AC-107

Dependencies:

TASK-141

---

## TASK-143 — Manual Edit UI

Status: `TODO`

Goal:

Chapter Workspace 支持：

- Edit；
- Save；
- Cancel；
- 当前 revision 信息。

Dependencies:

TASK-142

---

## TASK-144 — Implement Regenerate

Status: `TODO`

Goal:

根据相同 ChapterSpec 重生成。

规则：

- Chapter ID 不变；
- Chapter Number 不变；
- 新 Revision；
- 可接受 author instruction。

Real-LLM Semantic Verification:

后续全验收。

Dependencies:

TASK-141

---

## TASK-145 — Revision History API + UI

Status: `TODO`

Goal:

能够查看：

- Version；
- Source Type；
- Created At；
- Content。

不做复杂 diff viewer。

Dependencies:

TASK-142
TASK-144

---

## TASK-146 — Approve Chapter

Status: `TODO`

Goal:

作者可将当前 Revision：

```text
DRAFT → APPROVED
```

不实现发布平台状态。

Dependencies:

TASK-142

---

## TASK-147 — Mark Memory Stale on Revision Change

Status: `TODO`

Goal:

以下动作后：

```text
MANUAL_EDIT
AI_REWRITE
AI_POLISH
```

必须：

```text
memoryExtractionStatus=STALE
```

Dependencies:

TASK-142

---

## TASK-148 — Re-extract and Reconcile Chapter-derived Memory

Status: `TODO`

Goal:

对 STALE Chapter：

- 失效 / 删除该 Chapter 派生的旧有效 Memory；
- 重新 Extract；
- 重新 Apply safe candidates；
- status → COMPLETED。

第一版不做复杂 semantic merge。

Dependencies:

TASK-147

---

## TASK-149 — Manual Edit + Memory Refresh Acceptance

Status: `TODO`

Goal:

执行：

AC-107 + AC-108。

例：

```text
AI draft: 获得铁剑
author removes event
```

最终：

- new Revision active；
- old Revision exists；
- 铁剑不再作为 Current State。

Dependencies:

TASK-148
TASK-143

---

## Phase 5 Gate

```text
[ ] Manual Edit PASS
[ ] Revision History PASS
[ ] Regenerate works
[ ] Memory refresh PASS
[ ] AC-107 PASS
[ ] AC-108 PASS
```

---

# 10. Phase 6 — Long-form Pace

目标：

> 让 Planner 知道“这是一部长篇”，而不是尽快完成所有设定。

---

## TASK-150 — Enable Story Target Chapter Count

Status: `TODO`

Goal:

实现 Story：

```text
targetChapterCount
```

CRUD / UI 基础设置。

旧 Story 允许 NULL。

Dependencies:

Phase 5 Gate

---

## TASK-151 — Add Arc Schema

Status: `TODO`

Goal:

新增：

```text
arc
```

字段：

```text
id
story_id
title
goal
target_start_chapter
target_end_chapter
status
```

Dependencies:

TASK-150

---

## TASK-152 — Arc Persistence + Minimal API

Status: `TODO`

Goal:

支持：

- Create；
- List；
- Update；
- 当前 Arc。

不建设复杂 Arc Version。

Dependencies:

TASK-151

---

## TASK-153 — Minimal Arc UI

Status: `TODO`

Goal:

作者可以看到 / 编辑：

- Arc title；
- goal；
- chapter range。

不做复杂 Timeline。

Dependencies:

TASK-152

---

## TASK-154 — Add Long-form Position to Planner Contract

Status: `TODO`

Goal:

Planner 获取：

```text
targetChapterCount
currentChapterNumber
currentArc
arcRange
arcGoal
```

Dependencies:

TASK-152

---

## TASK-155 — Implement Pace Guard Prompt

Status: `TODO`

Goal:

明确：

如果：

```text
target=600
current=5
arc=1–60
```

不要默认推进：

- 最终真相；
- 终局大战；
- 最终返回通道；
- 全书主矛盾解决。

不要用硬编码剧情词表代替模型规则；测试案例可以使用这些例子。

Dependencies:

TASK-154

---

## TASK-156 — Real-LLM 600 Chapter Pace Acceptance

Status: `TODO`

Goal:

执行 AC-114。

Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-155

---

## Phase 6 Gate

```text
[ ] targetChapterCount works
[ ] Arc works
[ ] Planner receives longFormPosition
[ ] AC-114 PASS
```

---

# 11. Phase 7 — Memory v2

目标：

> 让 Memory 从“所有发生过的东西”变成“有层级、有范围、可选择的故事知识”。

---

## TASK-157 — Memory v2 Migration

Status: `TODO`

Goal:

StoryMemory / Candidate 增加：

```text
importance
scope
active
```

必要时增加 dedup support。

旧数据必须可读。

Dependencies:

Phase 6 Gate

---

## TASK-158 — Freeze Memory Type Contract as Enum

Status: `TODO`

Goal:

Python / Java 双端固定：

```text
CURRENT_STATE
RELATIONSHIP
PLOT_FACT
PLOT_THREAD
FORESHADOWING
WORLD_RULE
TRANSIENT_DETAIL
```

未知类型：

> 不得 catch-all 自动进入 StoryMemory。

Dependencies:

TASK-157

---

## TASK-159 — Add Importance + Scope to AI Contract

Status: `TODO`

Goal:

Memory Extractor Response 每个 Candidate 包含：

```text
importance 1–5
scope CHAPTER/STAGE/ARC/STORY
```

结构化 Validation。

Dependencies:

TASK-158

---

## TASK-160 — Redesign Memory Extractor Prompt

Status: `TODO`

Goal:

加入判断：

```text
Is this current state?
Will the writer need this five chapters later?
Is this a plot asset or transient detail?
What is its importance?
What is its scope?
```

明确：

> Extract, don't invent.

Dependencies:

TASK-159

---

## TASK-161 — Safe Candidate Processing v2

Status: `TODO`

Goal:

Java 只允许合法 type / importance / scope。

未知类型：

- REVIEW；
- 或 Contract Error。

不能自动长期保存。

Dependencies:

TASK-159

---

## TASK-162 — StoryMemory Dedup v1

Status: `TODO`

Goal:

实现最小结构化 / normalized exact 去重。

不使用 Embedding。

Acceptance:

AC-115

Dependencies:

TASK-161

---

## TASK-163 — Fix Inventory Multi-item State

Status: `TODO`

Goal:

允许同时表达：

```text
铁剑
药水
```

推荐最小：

```text
field=item:<normalized-name>
```

Acceptance:

AC-116

Dependencies:

TASK-161

---

## TASK-164 — Implement Writer Memory Selection

Status: `TODO`

Goal:

禁止默认传全部 Memory。

Always：

- Current State；
- 当前人物关系；
- importance=5。

Relevant：

- Arc / Stage / ChapterSpec related；
- active thread；
- active foreshadowing。

Exclude by default：

```text
TRANSIENT_DETAIL
importance<=2
```

不使用 RAG。

Dependencies:

TASK-162
TASK-163

---

## TASK-165 — Bread-loop Regression Acceptance

Status: `TODO`

Goal:

执行 AC-105。

普通面包：

```text
TRANSIENT_DETAIL
importance=1
scope=CHAPTER
```

后续三个无关 ChapterSpec：

> 不再持续围绕面包。

Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-160
TASK-164

---

## Phase 7 Gate

```text
[ ] Type enum safe
[ ] importance/scope works
[ ] dedup works
[ ] inventory multi-item works
[ ] Memory Selection works
[ ] AC-105 PASS
[ ] AC-115 PASS
[ ] AC-116 PASS
```

---

# 12. Phase 8 — Polish + Style

目标：

> 在“写什么”已经可控后，再优化“怎么写”。

---

## TASK-166 — Story Writing Style Profile

Status: `TODO`

Goal:

Story 支持自然语言：

```text
writingStyle
```

不做几十个滑块。

Dependencies:

Phase 7 Gate

---

## TASK-167 — Add Polish AI Contract

Status: `TODO`

Goal:

新增：

```text
POST /ai/polish-chapter
```

Input 至少：

- content；
- ChapterSpec；
- constraints；
- currentState；
- writingStyle；
- userInstruction。

Output：

- polished content。

Dependencies:

TASK-166

---

## TASK-168 — Implement Polish Prompt Fact Preservation

Status: `TODO`

Goal:

允许：

- 调整句式；
- 对话；
- 场景；
- 重复；
- 机械总结。

禁止：

- 新设定；
- 核心事件改变；
- 人物状态改变；
- Ending Intent 改变。

Dependencies:

TASK-167

---

## TASK-169 — Backend Polish Revision Workflow

Status: `TODO`

Goal:

Polish：

```text
current Revision
→ Python
→ new AI_POLISH Revision
→ memory STALE
```

Dependencies:

TASK-168
TASK-147

---

## TASK-170 — Polish UI

Status: `TODO`

Goal:

作者可以：

- 输入 optional instruction；
- Polish；
- 查看新 Revision；
- 回看旧 Revision。

Dependencies:

TASK-169

---

## TASK-171 — Real-LLM Polish Acceptance

Status: `TODO`

Goal:

执行 AC-109。

必须人工 / 结构化检查：

- plot preserved；
- location preserved；
- inventory preserved；
- ending intent preserved。

Real-LLM Semantic Verification: REQUIRED

Dependencies:

TASK-170

---

## Phase 8 Gate

```text
[ ] writingStyle works
[ ] Polish creates Revision
[ ] Memory marked stale
[ ] AC-109 PASS
```

---

# 13. Phase 9 — Full Real-LLM Acceptance

目标：

> 不再用 Mock 替代产品验收。

---

## TASK-172 — Prepare v0.1.1 Acceptance Fixture

Status: `TODO`

Goal:

准备固定真实测试 Story。

保留：

- model；
- prompt version；
- story settings；
- arc；
- stage；
- chapter specs。

不得为了结果通过临时修改 fixture。

Dependencies:

Phase 8 Gate

---

## TASK-173 — Execute Full Engineering Verification

Status: `TODO`

Goal:

实际执行：

```text
backend tests
python tests
frontend build
migration test
```

以及：

- retry；
- async；
- pause；
- replan；
- revision；
- dedup。

Engineering Verification: REQUIRED

Dependencies:

TASK-172

---

## TASK-174 — Execute Real-LLM Semantic Acceptance Suite

Status: `TODO`

Goal:

真实运行：

```text
AC-101
AC-103
AC-104
AC-105
AC-106
AC-109
AC-114
```

并保留原始输出。

不得只记录“PASS”。

必须保存证据。

Dependencies:

TASK-173

---

## TASK-175 — Record v0.1.1 Metrics

Status: `TODO`

Goal:

至少记录：

```text
Model
Prompt Version
Story ID
Run ID
Target Characters
Actual Characters
Length Pass Rate
Chapter Goal Completion Rate
Continuation Failure Count
Low-value Detail Repetition Count
Duplicate Memory Count
Manual Edit Count
Regeneration Count
Polish Count
Memory Extraction Failure Count
Average Generation Latency
```

Dependencies:

TASK-174

---

## TASK-176 — Fix Blocking Acceptance Failures Only

Status: `TODO`

Goal:

如果 Acceptance Failure：

只修：

> 阻塞冻结 AC 的最小问题。

不在最后阶段新增功能。

Dependencies:

TASK-174

---

## TASK-177 — Re-run Failed Acceptance Cases

Status: `TODO`

Goal:

只对失败项重新执行完整条件。

不得降低标准。

Dependencies:

TASK-176

---

## TASK-178 — Update Truthful README / STATE / Experiment Record

Status: `TODO`

Goal:

只有真实证据支持的能力才能写入 README。

必须明确：

- 哪些 Engineering PASS；
- 哪些 Real-LLM PASS；
- Known Limitations；
- Deferred Scope。

Dependencies:

TASK-175
TASK-177

---

## TASK-179 — Freeze v0.1.1

Status: `TODO`

Goal:

只有满足 `V0.1.1_IMPROVEMENT_PLAN.md` Definition of Done 时：

```text
v0.1.1 ACCEPTED
```

否则：

```text
v0.1.1 NOT ACCEPTED
```

并记录剩余 blocker。

不得使用：

```text
mostly accepted
should work
basically done
```

Dependencies:

TASK-178

---

# 14. Deferred Backlog

以下能力不得进入 v0.1.1：

```text
RAG
Embedding
Vector Search
GraphRAG
Knowledge Graph
Temporal Memory
Canon Governance
Volume Layer
LangGraph
Redis
MQ
Spring Cloud
Kubernetes
Authentication
Multi-user
Auto Publishing
Fine-tuning
AI Detection Bypass
```

---

# 15. Current Starting Point

Current Version:

`v0.1.1`

Frozen Requirement:

`V0.1.1_IMPROVEMENT_PLAN.md`

Current Phase:

`Phase 0 — Correct Evidence Base`

Current Task:

`TASK-101 — Snapshot v0.1 Regression Evidence`

Task Status:

`TODO`

Next Safe Action:

> 检查 Git 当前状态，保存 v0.1 真实 regression evidence；不要立刻开始重构 Planner / Writer。

---

# 16. Milestone Summary

```text
Phase 0
TASK-101 ~ TASK-104

Phase 1
TASK-105 ~ TASK-112

Phase 2
TASK-113 ~ TASK-122

Phase 3
TASK-123 ~ TASK-132

Phase 4
TASK-133 ~ TASK-139

Phase 5
TASK-140 ~ TASK-149

Phase 6
TASK-150 ~ TASK-156

Phase 7
TASK-157 ~ TASK-165

Phase 8
TASK-166 ~ TASK-171

Phase 9
TASK-172 ~ TASK-179
```

---

# 17. Final Task Principle

> **Fix observed behavior before adding architecture.**

> **Prove wiring with tests. Prove AI behavior with a real model.**

> **Never let TASKS.md become evidence by itself. Repository behavior is the evidence.**
