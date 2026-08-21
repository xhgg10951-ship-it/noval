# .agent/TASKS.md

## 0. Purpose

本文件是 **AI Story Co-Author v0.1** 的当前开发任务看板。

本文件由 Coding Agent 持续维护。

它回答：

- 当前要做什么；
- 哪些任务已经完成；
- 哪些任务正在进行；
- 哪些任务被阻塞；
- 当前 Milestone 的完成条件是什么；
- 下一步最小安全任务是什么。

本文件不是长期产品 Roadmap。

v0.1 范围以：

- `MVP_SCOPE.md`
- `PRODUCT_SPEC.md`
- `ACCEPTANCE_TESTS.md`

为准。

---

# 1. Task Management Rules

## 1.1 One Active Task

任何时间只能有一个主要实施任务处于：

`IN_PROGRESS`

状态。

如果一个任务过大，必须先拆分。

---

## 1.2 Task Status

允许状态：

- `TODO`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`
- `DEFERRED`

不得使用含糊状态：

- Almost Done
- Mostly Done
- 90%
- Probably Complete

---

## 1.3 DONE Definition

任务只有同时满足以下要求才能标记 `DONE`：

- 实现完成；
- 与任务相关的测试已完成；
- 实际执行验证；
- 验证通过；
- `.agent/STATE.md` 已更新；
- 不存在隐藏的必需后续工作。

如果测试尚未执行：

保持 `IN_PROGRESS`。

---

## 1.4 Task Scope

每个 Task 应尽可能：

> 一个 Agent 在有限上下文内可以理解、实现、验证。

不得建立：

`完成全部后端`

这类巨大任务。

---

## 1.5 Acceptance Reference

能够对应 `ACCEPTANCE_TESTS.md` 的 Task 应记录对应 Acceptance Test。

实现目标优先以 Acceptance Test 为准。

---

## 1.6 New Tasks

Agent 可以在开发过程中新增任务，但必须满足：

1. 属于现有 v0.1 Scope；
2. 或解决已经观察到的真实问题；
3. 不得通过新增 Task 偷偷扩大 MVP。

Scope 外能力必须进入：

`DEFERRED`

而不是当前实施计划。

---

## 1.7 Bugs

阻塞当前 Milestone 的 Bug：

立即创建 Task 并优先修复。

不阻塞核心闭环的小问题：

可记录到 `.agent/DEBT.md`。

如果 `.agent/DEBT.md` 尚不存在，可以在第一次真实技术债出现时创建。

---

# 2. Development Strategy

v0.1 按照：

> Vertical Slice First

开发。

开发顺序不是：

`全部数据库 → 全部后端 → 全部AI → 全部前端`

而是不断建立可以运行的小闭环。

总体顺序：

`项目启动`
→ `Story最小闭环`
→ `Planner闭环`
→ `单章生成闭环`
→ `Memory闭环`
→ `多章生成`
→ `Story Query / Planner Suggestions`
→ `验收实验`
→ `v0.1冻结`

---

# 3. Milestone Overview

## M0 — Repository Bootstrap

目标：

> 所有服务可以独立启动，并拥有最基本工程骨架。

---

## M1 — Story Vertical Slice

目标：

> 用户可以通过 Vue 创建 Story，Spring Boot 保存到 MySQL，并重新读取。

---

## M2 — Stage Planning Vertical Slice

目标：

> 用户输入 Stage Direction，系统调用 Python Planner，返回结构化 Chapter Plan，并保存。

---

## M3 — Single Chapter Generation

目标：

> 根据已经确认的 Chapter Plan 成功生成并保存一个 Chapter。

---

## M4 — Memory Vertical Slice

目标：

> Chapter 生成后能够提取 Memory Candidates，处理 AUTO / REVIEW / IGNORE，并维护基础 Current State。

---

## M5 — Multi-Chapter Generation

目标：

> Step-by-Step 和 Continuous 两种模式都能够基于同一单章流程工作。

---

## M6 — Author Assistance

目标：

> Planner Suggestions 与 Story Query 可使用当前 Story 信息辅助作者。

---

## M7 — Acceptance & Memory Experiment

目标：

> 完成固定 5 章测试和 Baseline vs Memory 实验。

---

## M8 — v0.1 Release Freeze

目标：

> 修复阻塞问题、整理 README/运行方式、记录 Known Issues，然后停止增加功能。

---

# 4. M0 — Repository Bootstrap

## TASK-001 — Initialize Repository Structure

Status: `DONE`

Goal:

创建基本 monorepo / project workspace：

```text
backend/
ai-service/
frontend/
.agent/
```

并确保核心文档位于仓库可访问位置。

Acceptance:

- 项目目录清晰；
- 不引入 Scope 外基础设施；
- Git 可以正常跟踪源码。

Dependencies:

None

---

## TASK-002 — Initialize Spring Boot Backend

Status: `DONE`

Goal:

创建：

- Java 21
- Spring Boot 3.5.x
- Maven

基础项目。

只加入当前必需 Dependency。

Initial Dependencies:

- Spring Web
- Validation
- MyBatis Spring Boot Starter
- MySQL Connector/J
- Test

Verification:

```bash
mvn test
```

以及 Spring Boot 能够启动。

Dependencies:

TASK-001

---

## TASK-003 — Initialize MySQL Development Database

Status: `DONE`

Goal:

建立本地 v0.1 MySQL 数据库配置。

Requirements:

- 配置通过环境变量或本地非提交配置；
- 仓库不得包含真实密码；
- 提供示例配置。

Verification:

Spring Boot 能成功连接数据库。

Dependencies:

TASK-002

---

## TASK-004 — Initialize Python AI Service

Status: `DONE`

Goal:

创建：

- Python 3.12+
- FastAPI
- Pydantic
- LangChain
- pytest

基础服务。

添加最简单 Health Endpoint。

Verification:

- 服务能够启动；
- Health endpoint 返回成功；
- `pytest` 能运行。

Dependencies:

TASK-001

---

## TASK-005 — Initialize Vue Frontend

Status: `DONE`

Goal:

创建：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios

基础应用。

Verification:

```bash
npm run build
```

通过。

Dependencies:

TASK-001

---

## TASK-006 — Add Backend ↔ AI Service Connectivity Check

Status: `DONE`

Goal:

Spring Boot 通过统一 AI Client 调用 Python Health Endpoint。

Purpose:

尽早验证 Java/Python 服务边界。

Verification:

Spring Boot 测试或手动调用能够收到 Python 成功响应。

Dependencies:

TASK-002
TASK-004

---

# 5. M1 — Story Vertical Slice

## TASK-007 — Design Minimum Story Schema

Status: `DONE`

Goal:

只设计当前 Story 创建所需的最小表结构。

至少覆盖：

- Story；
- Story Constraints。

禁止：

提前一次性设计全部 v0.1 表。

Dependencies:

TASK-003

---

## TASK-008 — Implement Story Persistence with MyBatis

Status: `DONE`

Goal:

实现 Story：

- Insert
- Select by ID
- Basic list

使用：

> Mapper Interface + XML

Verification:

Mapper / Service 测试通过。

Dependencies:

TASK-007

---

## TASK-009 — Implement Story REST API

Status: `DONE`

Goal:

实现最小：

- Create Story
- Get Story
- List Stories

包括基础 Validation 和错误处理。

Acceptance Reference:

AT-A01

Verification:

Spring Boot integration test。

Dependencies:

TASK-008

---

## TASK-010 — Implement Story Creation UI

Status: `DONE`

Goal:

Vue 支持：

- Story Name
- Core Idea
- 基础 Story Constraints
- Optional Initial Stage Direction

提交 Spring Boot API。

Acceptance Reference:

AT-A01

Verification:

真实浏览器流程：

Create → Refresh → Data Still Exists

Dependencies:

TASK-005
TASK-009

---

## M1 Completion Gate

必须证明：

```text
Vue
→ Spring Boot
→ MyBatis
→ MySQL
→ Spring Boot
→ Vue
```

完整工作。

在 M1 完成前：

不得开始复杂 Memory 或 Agent Workflow。

---

# 6. M2 — Stage Planning Vertical Slice

## TASK-011 — Add Stage and ChapterPlan Minimum Schema

Status: `TODO`

Goal:

增加当前规划流程所需最小持久化结构：

- Stage
- ChapterPlan

禁止加入未来复杂 Branch 系统。

Dependencies:

TASK-010

---

## TASK-012 — Implement Stage Persistence

Status: `TODO`

Goal:

实现：

- Create Stage
- Load Stage
- Update Stage status
- Save Chapter Plans

使用 MyBatis。

Dependencies:

TASK-011

---

## TASK-013 — Define Java ↔ Python Planner Contract

Status: `TODO`

Goal:

定义结构化：

`PlanStageRequest`

和：

`PlanStageResponse`

至少返回：

- suggestedChapterCount
- chapterPlans[]
- chapter order
- chapter goal
- expected progress

禁止 Java Regex 解析自然语言 Planner Response。

Dependencies:

TASK-006

---

## TASK-014 — Implement Python Stage Planner

Status: `TODO`

Goal:

使用 LangChain + Structured Output 实现 Stage Planner。

输入：

- Core Idea
- Story Constraints
- Stage Direction
- 当前必要 Story Context

输出：

结构化 Chapter Plan。

Acceptance Reference:

AT-B01

Verification:

pytest + 至少一个实际模型调用手动验证。

Dependencies:

TASK-013

---

## TASK-015 — Implement Spring Boot Stage Planning Service

Status: `TODO`

Goal:

实现：

```text
Load Story
→ Build Planner Request
→ Call Python
→ Validate Response
→ Save Stage Plan
```

Acceptance Reference:

AT-B01

Dependencies:

TASK-012
TASK-014

---

## TASK-016 — Implement Plan Adjustment / Replanning

Status: `TODO`

Goal:

用户可以：

- 5 章 → 2 章；
- 或其他目标数量；

系统重新生成完整计划，而不是简单裁掉多余章节。

Acceptance Reference:

AT-B02

Dependencies:

TASK-015

---

## TASK-017 — Implement Planning UI

Status: `TODO`

Goal:

Vue 可以：

- 输入 Stage Direction；
- 查看 AI Suggested Chapter Count；
- 查看 Chapter Plans；
- 修改目标章节数；
- Replan；
- Confirm Plan。

Acceptance Reference:

AT-B01
AT-B02
AT-B03

Dependencies:

TASK-016

---

# 7. M3 — Single Chapter Generation

## TASK-018 — Add Chapter Minimum Schema

Status: `TODO`

Goal:

保存：

- Story
- ChapterPlan
- Chapter Number
- Title
- Content
- Summary
- Generation Status

Chapter 必须保存完整正文。

Dependencies:

TASK-017

---

## TASK-019 — Implement Chapter Persistence

Status: `TODO`

Goal:

使用 MyBatis 实现：

- Save Chapter
- Get Chapter
- List Story Chapters

确保同一个 ChapterPlan 不会产生多个当前有效 Chapter。

Dependencies:

TASK-018

---

## TASK-020 — Define Writer AI Contract

Status: `TODO`

Goal:

定义：

`GenerateChapterRequest`

至少包含：

- Story Constraints
- Stage Direction
- Chapter Goal
- Current State
- Story Memory
- Recent Context

Response：

- title
- content
- summary

Dependencies:

TASK-013

---

## TASK-021 — Implement Python Writer

Status: `TODO`

Goal:

实现最简单可靠 Writer。

不得实现 Autonomous Planning Loop。

Writer 只完成：

> 当前 Chapter Goal。

Acceptance Reference:

AT-C01
AT-D01
AT-D02
AT-D03

Dependencies:

TASK-020

---

## TASK-022 — Implement Java Writer Context Assembly v1

Status: `TODO`

Goal:

v1 Context 暂时由：

- Story Constraints
- Stage
- Chapter Plan
- Previous Chapter Summary

组成。

Memory 尚未完成时允许为空。

Purpose:

先证明生成链路，不提前等待 Memory。

Dependencies:

TASK-019
TASK-021

---

## TASK-023 — Implement Generate-One-Chapter Service

Status: `TODO`

Goal:

Spring Boot：

```text
Load Context
→ Call Writer
→ Validate Response
→ Persist Chapter
```

AI 调用必须位于长数据库事务之外。

Acceptance Reference:

AT-C01

Dependencies:

TASK-022

---

## TASK-024 — Implement Chapter Reading UI

Status: `TODO`

Goal:

Vue 可以：

- 查看 Chapter List；
- 查看正文；
- 查看 Summary；
- 手动生成当前下一章。

Acceptance Reference:

AT-C01

Dependencies:

TASK-023

---

## M3 Completion Gate

必须真实完成：

```text
Story
→ Stage
→ Plan
→ Generate One Chapter
→ Save
→ Read in Vue
```

在此闭环工作之前：

不得开始 Continuous Generation。

---

# 8. M4 — Memory Vertical Slice

## TASK-025 — Design Minimum Memory Schema

Status: `TODO`

Goal:

只设计当前需要：

- MemoryCandidate
- StoryMemory
- CurrentState
- RelationshipState

的数据结构。

必须支持：

- AUTO
- REVIEW
- IGNORE
- sourceChapterId
- evidenceText

禁止加入：

- Canon
- Authority
-完整 Temporal Model
- Vector Embedding

Dependencies:

TASK-024

---

## TASK-026 — Implement Memory Persistence

Status: `TODO`

Goal:

MyBatis 实现：

- Save Candidate
- Query Pending Review
- Save Story Memory
- Query Story Memory
- Get / Upsert Current State
- Get / Update Relationship State

Dependencies:

TASK-025

---

## TASK-027 — Define Memory Extraction AI Contract

Status: `TODO`

Goal:

定义结构化 Request / Response。

Candidate 至少包含：

- type
- subject
- field / description
- value
- suggestedAction
- evidence

Acceptance Reference:

AT-G01
AT-G02
AT-G03
AT-G04

Dependencies:

TASK-020

---

## TASK-028 — Implement Python Memory Extractor

Status: `TODO`

Goal:

LangChain Structured Output。

核心 Prompt 原则：

> Extract, don't invent.

需要重点测试：

- Location
- Inventory
- Physical State
- Relationship
- Important Detail
- Potential Foreshadowing
- High Impact Setting

Acceptance Reference:

AT-G01
AT-G02
AT-G03
AT-G04

Dependencies:

TASK-027

---

## TASK-029 — Implement Memory Candidate Processing

Status: `TODO`

Goal:

Spring Boot 根据 Candidate：

```text
AUTO
→ safe apply

REVIEW
→ pending

IGNORE
→ ignored
```

不得简单：

`AUTO = 任意数据库修改`

只有明确允许类型才能改变 Current State。

Dependencies:

TASK-026
TASK-028

---

## TASK-030 — Implement Current State Update Rules v1

Status: `TODO`

Goal:

至少支持：

- LOCATION
- INVENTORY
- PHYSICAL CONDITION
- EMOTION / STATE

使用 Current Value First。

Acceptance Reference:

AT-E01
AT-E02
AT-E03
AT-E04

Dependencies:

TASK-029

---

## TASK-031 — Implement Relationship State v1

Status: `TODO`

Goal:

保存 Current Relationship Description。

避免模型把轻微态度变化自动扩大为重大关系变化。

Acceptance Reference:

AT-G03
AT-J03

Dependencies:

TASK-029

---

## TASK-032 — Connect Chapter Generation → Memory Extraction

Status: `TODO`

Goal:

单章完整流程变成：

```text
Generate
→ Save Chapter
→ Extract Memory
→ Save Candidates
→ Apply AUTO
→ Checkpoint
```

如果 Extraction Failure：

- Chapter 保留；
- 流程暂停；
- 不继续下一章。

Acceptance Reference:

AT-M02

Dependencies:

TASK-030
TASK-031

---

## TASK-033 — Add Memory to Writer Context

Status: `TODO`

Goal:

Writer Context 增加：

- Current State
- Relationship State
- Story Memory

Acceptance Reference:

AT-C02
AT-E01
AT-E03

Dependencies:

TASK-032

---

## TASK-034 — Implement Memory Review API

Status: `TODO`

Goal:

作者可以：

- Accept
- Edit then Accept
- Ignore
- Override AUTO/IGNORE 判断

Acceptance Reference:

AT-H01
AT-H02
AT-H03

Dependencies:

TASK-029

---

## TASK-035 — Implement Memory UI

Status: `TODO`

Goal:

Vue 至少显示：

- Current State
- Story Memory
- Review Queue
- Candidate Evidence

Acceptance Reference:

AT-H01
AT-H02
AT-H03
AT-I01
AT-I02

Dependencies:

TASK-034

---

# 9. M5 — Multi-Chapter Generation

## TASK-036 — Add GenerationJob Persistence

Status: `TODO`

Goal:

建立最小 GenerationJob：

- stage
- mode
- currentPlanIndex
- total
- status
- lastError

状态至少支持：

- PENDING
- RUNNING
- PAUSED
- COMPLETED
- FAILED

Dependencies:

TASK-032

---

## TASK-037 — Refactor Generate-One-Chapter as Reusable Unit

Status: `TODO`

Goal:

确保：

Step-by-Step

和：

Continuous

使用完全相同的单章生成流程。

不得复制两套 Generation 实现。

Dependencies:

TASK-036

---

## TASK-038 — Implement Step-by-Step Mode

Status: `TODO`

Goal:

每章 checkpoint 后：

`PAUSED`

用户主动 Continue。

Acceptance Reference:

AT-L01

Dependencies:

TASK-037

---

## TASK-039 — Implement Continuous Mode

Status: `TODO`

Goal:

自动：

```text
Chapter
→ Memory
→ Checkpoint
→ Next Chapter
```

直到 Stage Complete 或 Failure。

Acceptance Reference:

AT-L02

Dependencies:

TASK-037

---

## TASK-040 — Implement Generation Failure / Retry

Status: `TODO`

Goal:

至少处理：

- Writer Failure
- Memory Extraction Failure
- AI Service unavailable
- Invalid Structured Response

支持安全 Retry。

Acceptance Reference:

AT-M01
AT-M02
AT-M03

Dependencies:

TASK-039

---

## TASK-041 — Implement Generation Progress UI

Status: `TODO`

Goal:

显示：

- 当前 Chapter / Total
- Planning / Writing / Memory 状态
- PAUSED / FAILED / COMPLETE
- Retry

Acceptance Reference:

Product Spec Generation Progress

Dependencies:

TASK-040

---

# 10. M6 — Author Assistance

## TASK-042 — Implement Planner Direction Suggestions

Status: `TODO`

Goal:

Python 返回至少 3 个明显不同的剧情候选。

不得自动修改 Stage。

Acceptance Reference:

AT-K01
AT-K02
AT-K03

Dependencies:

TASK-033

---

## TASK-043 — Implement Planner Suggestions API + UI

Status: `TODO`

Goal:

用户可以：

- 请求建议；
- 选择一个；
- 修改后作为新 Stage Direction；
- 全部拒绝。

Dependencies:

TASK-042

---

## TASK-044 — Implement Story Query Context Assembly

Status: `TODO`

Goal:

Spring Boot 根据 Story 提供：

- Current State
- Relationships
- Story Memory
- Source Evidence
- 必要 Summary

不加入 RAG。

Dependencies:

TASK-033

---

## TASK-045 — Implement Python Story Query

Status: `TODO`

Goal:

支持基础问题：

- Current Location
- Inventory
- Relationship
- Foreshadowing
- Source Explanation

如果没有信息：

返回 Unknown。

Acceptance Reference:

AT-J01
AT-J02
AT-J03
AT-J04
AT-J05

Dependencies:

TASK-044

---

## TASK-046 — Implement Story Query UI

Status: `TODO`

Goal:

提供简单自然语言查询交互。

Dependencies:

TASK-045

---

# 11. M7 — Acceptance & Memory Experiment

## TASK-047 — Prepare Fixed Acceptance Story

Status: `TODO`

Goal:

按照 `ACCEPTANCE_TESTS.md` 固定：

- Core Idea
- Constraints
- Seed Facts

不得为测试通过随意修改测试事实。

Dependencies:

TASK-046

---

## TASK-048 — Execute Five-Chapter End-to-End Test

Status: `TODO`

Goal:

完成完整：

```text
Create
→ Plan
→ Generate
→ Memory
→ Continue
→ Query
→ Suggestions
```

至少 5 个连续章节。

Acceptance Reference:

PASS-01 ~ PASS-12

Dependencies:

TASK-047

---

## TASK-049 — Execute Baseline Run

Status: `TODO`

Goal:

禁用 Structured Current State / Story Memory。

保持其他条件尽量一致。

记录：

- observed errors
- corrections
- notes

Acceptance Reference:

Experiment N1

Dependencies:

TASK-047

---

## TASK-050 — Execute Memory-Enabled Run

Status: `TODO`

Goal:

启用 Structured Memory。

记录相同观察项目。

Acceptance Reference:

Experiment N1

Dependencies:

TASK-049

---

## TASK-051 — Compare Baseline vs Memory

Status: `TODO`

Goal:

不得只给主观结论。

至少记录：

- State Errors
- Inventory Errors
- Relationship Errors
- Constraint Violations
- Important Detail Recall
- Manual Corrections

结果无论好坏都如实保存。

Dependencies:

TASK-050

---

## TASK-052 — Record v0.1 Known Issues

Status: `TODO`

Goal:

记录：

- 什么有效；
- 什么无效；
- 什么尚未验证；
- 什么应该进入 v0.2。

禁止为了宣布成功隐藏失败。

Dependencies:

TASK-051

---

# 12. M8 — v0.1 Release Freeze

## TASK-053 — Run Full Verification

Status: `TODO`

Goal:

至少执行实际项目存在的：

```text
mvn test
pytest
npm run build
```

以及核心端到端验收。

Dependencies:

TASK-052

---

## TASK-054 — Verify Local Startup Documentation

Status: `TODO`

Goal:

新环境能够根据仓库说明启动：

- MySQL
- Spring Boot
- Python AI Service
- Vue

不得依赖未记录的人工步骤。

Dependencies:

TASK-053

---

## TASK-055 — Prepare Resume / README Evidence

Status: `TODO`

Goal:

只整理已经真实实现并验证的内容：

- Architecture
- Story Loop
- Memory Design
- Baseline vs Memory Experiment
- Known Limitations

不得夸大未验证能力。

Dependencies:

TASK-054

---

## TASK-056 — Freeze v0.1

Status: `TODO`

Goal:

确认：

- Acceptance complete；
- Known Issues recorded；
- 核心代码 verified。

然后：

> 停止向 v0.1 添加功能。

未来需求进入：

`v0.2 planning`

Dependencies:

TASK-055

---

# 13. Deferred by Default

以下不是当前 Task。

除非 Human Authority Documents 后续修改，否则保持：

`DEFERRED`

- RAG
- Embedding
- Vector Search
- Redis
- MQ
- LangGraph
- Temporal Memory
- Canon Governance
- Knowledge Graph
- Multi-Provider Routing
- Spring Cloud
- Docker deployment polish
- Advanced Rich Text Editor
- Authentication
- Multi-user

---

# 14. Current Starting Point

Current Milestone:

`M0 — Repository Bootstrap`

Current Task:

`TASK-001 — Initialize Repository Structure`

Task Status:

`TODO`

Next Safe Action:

> 创建仓库基础目录和核心工程骨架，不开始任何产品功能实现。