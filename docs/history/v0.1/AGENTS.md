# AGENTS.md

## 1. Purpose

本文档定义所有 Coding Agent 在 **AI Story Co-Author v0.1** 仓库中的工作规则。

所有进入本仓库执行以下工作的 AI Agent 均必须遵守本文档：

- 需求分析；
- 任务拆解；
- 编码；
- 重构；
- 测试；
- Bug 修复；
- 文档维护；
- Code Review；
- Development Recovery。

本文档的目标不是限制 Agent 完成工作。

目标是：

> **让 Agent 能够在明确边界内尽可能自主地持续开发，同时保证项目不会因为上下文丢失、需求漂移、过度设计或中断而失控。**

---

# 2. Core Development Principle

本项目遵循：

> **Human defines direction and boundaries. Agent executes autonomously inside those boundaries.**

即：

> 人类定义目标、范围与关键约束。

> Agent 在这些边界内负责拆解、实现、测试、修复和推进。

Agent 不应该因为普通实现细节频繁请求人工确认。

只有遇到本文档定义的真正 Blocker 时才需要停止并请求决策。

---

# 3. Document Authority

Agent 在做任何重要决策之前，必须理解当前项目文档的职责。

当前核心文档：

```text
PROJECT_VISION.md
MVP_SCOPE.md
PRODUCT_SPEC.md
ACCEPTANCE_TESTS.md
TECH_STACK.md
ARCHITECTURE.md
AGENTS.md
```

它们具有不同 Authority。

---

# 4. Authority Order

如果文档之间出现冲突，优先级如下：

```text
1. AGENTS.md
   Agent 工作行为与开发安全规则

2. MVP_SCOPE.md
   v0.1 范围边界

3. PRODUCT_SPEC.md
   v0.1 产品行为

4. ACCEPTANCE_TESTS.md
   v0.1 验收要求

5. ARCHITECTURE.md
   当前架构边界

6. TECH_STACK.md
   技术选型

7. PROJECT_VISION.md
   长期方向
```

注意：

> `PROJECT_VISION.md` 描述长期愿景，不得使用长期愿景扩大当前 `MVP_SCOPE.md`。

---

# 5. Human-Owned Documents

以下文档属于：

> **Human Authority Documents**

Coding Agent 在普通开发任务中：

> **MUST NOT 擅自修改。**

包括：

```text
PROJECT_VISION.md

MVP_SCOPE.md

PRODUCT_SPEC.md

ACCEPTANCE_TESTS.md

TECH_STACK.md

ARCHITECTURE.md

AGENTS.md
```

如果 Agent 发现其中：

- 存在矛盾；
- 无法实现；
- 真实测试证明某项设计明显错误；
- 某项内容严重阻塞开发；

Agent 必须：

1. 不擅自修改原文；
2. 在 `.agent/STATE.md` 记录问题；
3. 在 `.agent/TASKS.md` 将相关任务标记为 BLOCKED；
4. 给出最小必要决策问题；
5. 等待项目所有者决定。

---

# 6. Agent-Owned Documents

以下文档由 Coding Agent 创建并持续维护：

```text
.agent/TASKS.md

.agent/STATE.md
```

必要时还可以创建：

```text
.agent/DEBT.md

.agent/PROGRESS.md
```

但：

> 不得因为“文档越多越专业”而继续建立大量 Agent 管理文档。

如果某个新文档不能直接帮助：

- 执行；
- 验收；
- 恢复；
- Debug；

则默认不创建。

---

# 7. Mandatory Reading Order

一个没有可靠当前上下文的新 Agent 进入仓库时，必须按照以下顺序恢复项目理解：

```text
1. AGENTS.md

2. .agent/STATE.md

3. .agent/TASKS.md

4. MVP_SCOPE.md

5. PRODUCT_SPEC.md

6. ACCEPTANCE_TESTS.md

7. ARCHITECTURE.md

8. TECH_STACK.md
```

只有在需要理解长期产品方向时，再阅读：

```text
PROJECT_VISION.md
```

不要每次任务都重新详细分析全部长期愿景。

---

# 8. Before Starting Any Development Session

Agent 开始开发前必须检查：

```text
git status
```

以及最近提交：

```text
git log --oneline -5
```

然后读取：

```text
.agent/STATE.md
.agent/TASKS.md
```

确认：

- 当前 Milestone；
- 当前 Task；
- 上一个完成任务；
- 是否存在未提交修改；
- 当前验证状态；
- 是否存在已知失败；
- 下一步安全动作。

---

# 9. Session Recovery Protocol

如果当前 Agent 无法确认之前会话是否正常结束，必须执行 Recovery Protocol。

步骤：

```text
1. Read AGENTS.md

2. Read .agent/STATE.md

3. Read .agent/TASKS.md

4. Run git status

5. Run git diff

6. Read recent git log

7. Identify current task

8. Inspect modified files

9. Run the smallest relevant verification

10. Determine current state:

   COMPLETE
   IN_PROGRESS
   BROKEN
   BLOCKED

11. Resume from the smallest safe next action
```

禁止：

> 因为不理解当前状态而直接删除未提交修改并重新开始。

---

# 10. Git Is Part of Recovery State

Git 不只是源码存储。

Git 是 Agent Recovery System 的一部分。

Agent 应保持：

> 一个已经验证完成的原子任务对应一个清晰 Commit。

推荐流程：

```text
Task
↓
Implement
↓
Test
↓
Verify
↓
Update Agent State
↓
Commit
↓
Next Task
```

---

# 11. One Task at a Time

Agent 必须遵循：

> **One active implementation task at a time.**

禁止一次同时开始：

```text
数据库设计
+
前端页面
+
AI Prompt
+
Generation Workflow
+
RAG
```

除非这些修改属于同一个不可拆分的最小垂直功能。

---

# 12. Task Size

`.agent/TASKS.md` 中的任务应尽可能满足：

> 一个 Agent 可以在有限上下文内理解、实现和验证。

好的任务：

```text
实现 Story 创建 REST API
```

```text
实现 Stage Plan 数据持久化
```

```text
实现 Python /ai/plan-stage endpoint
```

```text
完成 Spring Boot 对 Planner API 的调用
```

不好的任务：

```text
完成整个后端
```

```text
实现 AI 系统
```

```text
完成 Memory
```

---

# 13. Vertical Slice Priority

开发顺序优先：

> **Vertical Slice > 完整基础设施层。**

例如优先做到：

```text
Create Story
→ MySQL
→ API
→ Vue
```

能够完整运行。

而不是：

```text
先创建所有30张数据库表

然后写全部Mapper

然后写全部Service

然后最后才第一次启动系统
```

---

# 14. Build the Simplest Working Version First

面对一个需求时：

Agent 必须优先实现：

> **满足当前 Acceptance Test 的最简单可靠方案。**

例如：

当前 Memory 数量很少。

不要因为未来可能有 100,000 条 Memory 就立即引入：

```text
Vector Database
Redis
Elasticsearch
复杂 Ranking
```

先使用：

```text
MySQL
+
结构化查询
```

完成当前实验。

---

# 15. Scope Guard

Coding Agent 必须把：

```text
MVP_SCOPE.md
```

视为 v0.1 的范围防火墙。

Agent 不得因为认为以下能力“以后会需要”而自动加入：

```text
Redis
MQ
Spring Cloud
LangGraph
Vector Database
RAG
Knowledge Graph
Temporal Engine
Canon Governance
Complex Agent Framework
```

是否未来有价值与是否属于 v0.1：

> 是两个不同问题。

---

# 16. No Architecture Astronautics

禁止提前解决：

> 尚未被真实测试观察到的问题。

例如：

当前只有 5 章测试。

不得自动建立：

```text
Million-Chapter Storage Architecture

Distributed Memory Retrieval

Multi-Region Database

Complex Event Sourcing

High Availability LLM Router
```

架构复杂度必须由真实问题推动。

---

# 17. Technology Lock

当前 v0.1 核心技术栈已经确定：

```text
Frontend:
Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios

Backend:
Java 21
Spring Boot 3.5.x
MyBatis
MySQL 8.4 LTS
Maven

AI:
Python 3.12+
FastAPI
Pydantic
LangChain
One LLM Provider
```

Agent 不得擅自替换：

```text
MyBatis → JPA

MySQL → PostgreSQL

Vue → React

LangChain Python → Spring AI

Spring Boot → FastAPI main backend
```

---

# 18. New Dependency Rule

引入新的核心 Dependency 前，Agent 必须判断：

### Question 1

现有依赖是否已经能够简单解决？

如果是：

> 不新增。

### Question 2

新增依赖是否直接解决当前真实问题？

如果不是：

> 不新增。

### Question 3

这个依赖是否显著增加维护或学习成本？

如果是：

> 优先寻找简单方案。

---

# 19. Explicitly Forbidden Technologies for v0.1

除非 Human Authority Document 被修改，否则不得加入：

```text
Redis

Kafka

RabbitMQ

RocketMQ

Spring Cloud

Nacos

Consul

Elasticsearch

Milvus

Pinecone

Qdrant

Neo4j

LangGraph

Kubernetes

JPA / Hibernate

MyBatis-Plus

Spring AI
```

以及任何等价复杂替代方案。

---

# 20. Architectural Boundary

必须保持：

> **AI proposes. Java decides. MySQL remembers.**

具体含义：

### Python AI Service

负责：

- Plan；
- Generate；
- Extract；
- Suggest；
- Reason。

### Spring Boot

负责：

- 业务规则；
- 状态推进；
- 数据保存；
- Memory 应用；
- Generation Workflow；
- API。

### MySQL

负责：

- 持久化正式业务状态。

---

# 21. Python Must Not Own Business State

Python AI Service 不得：

- 直接修改 MySQL 核心业务表；
- 自己决定 Stage 已完成；
- 自己决定 REVIEW Memory 已正式生效；
- 自己保存正式 Chapter；
- 自己改变 Current State。

Python 返回：

> Structured AI Result。

Spring Boot 负责应用。

---

# 22. Vue Must Not Own Business State

Pinia / Vue State 不得被视为正式业务来源。

刷新页面后：

> 必须能够从 Spring Boot + MySQL 重建正式状态。

---

# 23. LLM Must Not Own Memory

LLM Context、LangChain Chat History 或模型内部上下文：

> 不属于正式 Story Memory。

正式 Story State / Memory 必须来自 MySQL。

---

# 24. AI Service Implementation Style

优先：

```text
Structured Input
↓
Prompt
↓
LLM
↓
Structured Output
```

不要默认使用：

```text
Unlimited autonomous Agent
+
Tools
+
Self-reflection loop
+
Recursive planning
```

例如：

```text
Planner
Writer
Memory Extractor
Story Query
```

可以只是不同的 AI Service Functions。

“Agent”是产品职责概念，不要求技术上全部实现为 Autonomous Agent Runtime。

---

# 25. Structured Output Rule

只要 AI 结果后续需要程序处理：

> 优先返回结构化结果。

例如：

```text
Stage Plan

Memory Candidate

Planner Suggestion
```

不得让 Java 依赖脆弱 Regex 去解析自然语言格式。

---

# 26. Prompt Change Rule

Prompt 是业务行为的一部分。

Agent 可以在实现和测试过程中修改 Prompt。

但每次修改应该服务于：

- Acceptance Test；
- 已观察到的失败；
- Structured Output 稳定性；
- 明确产品行为。

禁止：

> 无目的不断增加 Prompt 长度。

---

# 27. Writer Rule

Writer 负责：

> 完成当前 Chapter Plan。

Writer 不得：

- 自主增加 Stage 章节数量；
- 自主修改作者确认的 Story Constraints；
- 自主修改 Stage Direction；
- 将未确认 REVIEW Memory 视为永久确定世界规则。

---

# 28. Planner Rule

Planner 可以：

- 建议章节数；
- 拆分 Stage；
- 提供未来剧情候选。

Planner 不可以：

- 未经作者确认直接修改 Stage；
- 自动生成长期新主线并强制采用；
- 自主改变 Core Idea。

---

# 29. Memory Extractor Rule

Memory Extractor 应遵循：

> **Extract, don't invent.**

必须区分：

> 正文明显表达的信息

和：

> AI 自己推测出的解释。

例如原文：

```text
艾琳没有以前那么排斥主角。
```

合理：

```text
敌意降低
```

不合理：

```text
艾琳爱上主角
```

---

# 30. Memory Candidate Rule

Memory Candidate 初始只使用：

```text
AUTO
REVIEW
IGNORE
```

不得擅自扩展：

```text
CANONICAL
AUTHORITATIVE
SUPERSEDED
TEMPORAL
BLOCKING
```

除非真实测试证明当前三个状态不足，并由 Human Authority 文档批准。

---

# 31. AUTO Safety Rule

`AUTO` 不等于：

> AI 可以任意修改业务状态。

只有 Architecture / Product Spec 明确允许的低风险类型可以自动应用。

例如：

```text
Location
Inventory
Explicit Physical State
Certain factual events
```

高影响设定：

> 仍然进入 REVIEW。

---

# 32. REVIEW Rule

REVIEW Candidate：

> 作者确认前不得成为确定长期高影响事实。

Continuous Mode 可以暂时继续处理非阻塞 REVIEW。

不得为了提高自动化程度而偷偷接受 REVIEW。

---

# 33. Source Evidence Rule

由 Chapter 提取出的 Memory 应尽可能保留：

```text
sourceChapterId

evidenceText
```

Memory 不得成为无法追踪依据的 AI 声明。

---

# 34. Chapter Rule

完整 Chapter 正文必须保存。

不得：

> 只保存摘要，然后删除正文。

Chapter 是原始创作内容。

Memory 是 AI 的结构化理解。

两者职责不同。

---

# 35. Current State Rule

v0.1 采用：

> Current Value First。

只优先解决：

> “现在是什么？”

不要自动实现完整历史状态图。

如果旧状态已经失效：

> 不得继续作为 Current State 提供给 Writer。

---

# 36. Generation Workflow Rule

Continuous Mode 和 Step-by-Step Mode：

> 必须共享同一套 Generate-One-Chapter 流程。

不要维护两套完全不同实现。

概念：

```text
Generate One Chapter
↓
Persist
↓
Extract Memory
↓
Apply State
↓
Checkpoint
```

Continuous：

> 自动继续。

Step-by-Step：

> Checkpoint 后暂停。

---

# 37. One Chapter = Recovery Boundary

产品运行中的最小稳定恢复点：

> **One Completed Chapter**

每一章完成后必须尽可能形成一致状态：

```text
Chapter saved
+
Memory extraction processed
+
Current state updated
+
Generation job advanced
```

然后才能开始下一章。

---

# 38. AI Call and Transaction Rule

绝对禁止：

```text
Begin DB Transaction
↓
Wait LLM 30–120 seconds
↓
Commit
```

LLM 网络调用必须位于长数据库事务之外。

推荐：

```text
Read State

↓

Call AI

↓

Receive Result

↓

Short DB Transaction

↓

Persist
```

---

# 39. Failure Must Be Visible

如果：

- Planner 失败；
- Writer 失败；
- Memory Extraction 失败；
- Python Service 不可达；
- Structured Output 无法解析；

不得：

> 静默跳过。

必须产生明确失败状态。

---

# 40. Memory Extraction Failure Rule

如果：

```text
Chapter 已成功保存
```

但：

```text
Memory Extraction 失败
```

则：

> Chapter 保留。

Generation 必须暂停在一致恢复点。

不得继续生成下一章并假装 Memory 已更新。

---

# 41. Retry Rule

Retry 必须避免创建重复业务数据。

例如：

> 同一个 ChapterPlan Retry 后不能产生两个当前有效 Chapter。

实现时应该存在足够约束确保：

```text
One ChapterPlan
→
One current Chapter result
```

---

# 42. Error Handling Rule

不得把所有异常统一：

```text
catch Exception
→
return "failed"
```

至少逻辑区分：

```text
Validation Error

Business Error

AI Service Error

Database Error

Unexpected Internal Error
```

---

# 43. No Silent Fallback

如果 AI Service 失败：

不得偷偷：

> 返回假数据让流程继续。

如果 Memory 不存在：

不得：

> 编造 Memory。

如果 Query 找不到答案：

应返回：

> Unknown / 当前作品尚未确定。

---

# 44. Testing Is Part of Implementation

一个 Task 不满足：

> “代码写完”

就算完成。

Task 完成至少要求：

```text
implementation
+
relevant test
+
verification
```

---

# 45. Acceptance Tests Have Priority

实现某个功能前：

优先找到：

```text
ACCEPTANCE_TESTS.md
```

对应验收场景。

如果实现和 Acceptance Test 冲突：

> 实现必须调整。

除非 Acceptance Test 本身被 Human Authority 修改。

---

# 46. Minimum Verification Before DONE

根据修改范围执行相关验证。

Java 修改至少考虑：

```text
mvn test
```

Python 修改至少考虑：

```text
pytest
```

Frontend 修改至少考虑：

```text
npm run build
```

如果项目后续定义统一 Verify Script：

> 优先运行统一脚本。

---

# 47. Never Claim Tests Passed Without Running Them

Agent 不得使用：

> “看起来应该能通过。”

代替真实执行。

只能记录：

```text
PASSED
```

如果命令实际成功。

否则必须明确：

```text
NOT RUN
```

或：

```text
FAILED
```

---

# 48. Fix Before Expanding

如果当前核心流程存在失败：

例如：

```text
Chapter Generation → Memory Extraction
```

无法运行。

Agent 不得继续开发：

```text
Planner UI
RAG
高级Memory筛选
```

优先修复核心链路。

---

# 49. Refactoring Rule

允许重构，但只能满足：

- 修复真实问题；
- 明显降低复杂度；
- 支撑正在实现的需求；
- 清除已经影响开发的技术债。

禁止：

> 因为 Agent 个人偏好重写已经工作的系统。

---

# 50. No Large Opportunistic Refactors

正在完成：

```text
Memory Review
```

时不要顺便：

```text
重写全部 Error Handling

重命名所有 Package

迁移数据库框架
```

如果发现问题：

记录到：

```text
.agent/DEBT.md
```

然后继续当前任务。

---

# 51. Existing Code First

新增功能前：

Agent 应先搜索现有代码。

优先：

> 扩展已有模式。

不要自动创建：

```text
StoryService2

NewMemoryManager

BetterAiClient

FinalGenerationService
```

避免 AI Spaghetti Code。

---

# 52. No Duplicate Abstractions

实现前检查是否已经存在：

- DTO；
- Service；
- Mapper；
- Utility；
- API Client；
- Schema；
- Prompt builder。

不要因为没有读代码而重复创建。

---

# 53. Simplicity Over Generic Frameworks

不要为了两个类建立：

```text
GenericUniversalMemoryProcessorFactory
```

不要为了四个 AI endpoint 建：

```text
DynamicAgentPluginRegistry
```

v0.1 优先直接、可读的代码。

---

# 54. Comments Rule

Comment 应解释：

> 为什么。

而不是重复：

> 代码做什么。

好的：

```text
// AI calls are intentionally outside the DB transaction
// because provider latency can be tens of seconds.
```

不好的：

```text
// Call AI
callAi();
```

---

# 55. No Placeholder Production Logic

不得用：

```text
TODO return mock
```

然后把 Task 标记 DONE。

Mock 可以用于早期集成开发。

但 `.agent/STATE.md` 必须明确：

```text
MOCK
```

而不是宣称功能完成。

---

# 56. Secrets Rule

绝对禁止提交：

```text
API Key

Database Password

Private Token
```

真实值。

必须通过：

```text
environment variables
```

管理。

仓库只保留 example 配置。

---

# 57. Logging Rule

关键 Generation 流程建议带：

```text
storyId
stageId
chapterPlanId
chapterId
generationJobId
```

但不得写出：

> API Key。

---

# 58. Agent State File

`.agent/STATE.md` 必须保持短而准确。

它回答：

> **现在项目处于什么状态？**

而不是完整历史日志。

推荐格式：

```text
# Current State

Current Milestone:
...

Current Task:
...

Task Status:
IN_PROGRESS

Last Verified Commit:
...

Last Successful Verification:
...

Currently Modified:
...

Known Failures:
...

Current Blockers:
...

Next Safe Action:
...
```

---

# 59. State Update Timing

至少在以下时机更新 `.agent/STATE.md`：

### 开始新的 Task

记录：

```text
Current Task
```

### Task 完成

记录：

```text
Verification
Commit
Next Task
```

### 出现 Blocker

立即记录。

### 会话可能结束前

确保新 Agent 能恢复。

---

# 60. TASKS.md

`.agent/TASKS.md` 是：

> v0.1 当前实现计划。

推荐：

```text
## TODO

TASK-001 ...
TASK-002 ...

## IN PROGRESS

TASK-003 ...

## DONE

TASK-000 ...
```

每个任务尽量包含：

```text
Goal
Acceptance Reference
Dependencies
Status
```

---

# 61. DONE Means DONE

任务只有同时满足：

```text
Code implemented

Relevant test exists

Verification passed

State updated
```

才能移动到 DONE。

如果仍然：

```text
"基本完成"
"应该没问题"
"等之后一起测"
```

必须保持 IN_PROGRESS。

---

# 62. Blocker Definition

只有以下情况属于需要人类介入的真正 Blocker：

### Product Conflict

两个 Human Authority 文档明确冲突。

### Scope Decision

实现核心验收必须突破 MVP Scope。

### Technology Change

当前锁定技术确实无法实现需求，需要更换核心技术。

### High-Impact Product Decision

存在多个方案，选择会明显改变产品行为，而现有文档无法判断。

### Secret / External Access

缺少必须由用户提供的：

- API Key；
- 外部账户权限；
- 服务凭证。

### Destructive Action

需要：

- 删除大量用户数据；
- 重写不可恢复数据；
- 进行高风险迁移。

---

# 63. What Is NOT a Blocker

以下普通工程问题不应该频繁找用户：

```text
类叫什么名字

DTO 放哪个 package

普通 SQL 怎么写

测试怎么组织

函数怎么拆

变量如何命名

哪个小组件怎么布局
```

Agent 应根据现有代码和工程惯例自行判断。

---

# 64. Autonomous Debugging Rule

遇到普通失败：

Agent 应自主：

```text
Read Error

↓

Find Cause

↓

Implement Smallest Fix

↓

Run Test

↓

Repeat
```

不要因为第一次测试失败就立即停止找用户。

---

# 65. Debugging Loop Limit

Agent 不应无限重复同一个失败方案。

如果连续多次修复：

> 没有产生新的有效信息，

必须：

1. 停止盲目尝试；
2. 重新分析假设；
3. 查看 Git diff；
4. 查看相关文档；
5. 尝试更小复现；
6. 必要时记录 Blocker。

---

# 66. Preserve Working State

在进行高风险重构之前：

> 先确保当前工作状态可通过 Git 恢复。

不要在：

```text
大量未提交修改
```

基础上进行第二次大型改造。

---

# 67. Scope Change Request Format

如果 Agent 真正认为必须改变 Scope：

必须说明：

```text
Observed Problem:

Why current scope cannot satisfy acceptance:

Smallest required change:

Alternatives considered:

Impact:

Documents affected:
```

禁止简单说：

> “建议加入 Redis，会更专业。”

---

# 68. Architecture Change Request Format

如果需要改变架构：

记录：

```text
Current Architecture:

Observed Failure:

Why current architecture is insufficient:

Minimal proposed change:

New complexity introduced:

How to verify the change:
```

---

# 69. Do Not Optimize for Resume Keywords

Agent 不得主动：

> “为了简历更好看”

而加入技术。

项目的简历价值来自：

- 真正的问题；
- 清晰架构；
- 实验；
- 可解释技术选择；
- 可靠实现。

不是技术数量。

---

# 70. Learning-Friendly Code Rule

由于项目同时用于 Java 后端求职学习：

Agent 应优先生成：

> 容易阅读、符合常见 Spring Boot / MyBatis 工程实践的实现。

禁止过度使用：

- 复杂元编程；
- 隐式魔法；
- 不必要高级抽象；
- 极端函数式写法；

导致开发者无法理解项目。

---

# 71. Java Specific Rule

Java 代码应优先保持：

```text
Controller
→
Service
→
Mapper
```

职责清晰。

复杂业务必须在 Service。

Mapper 负责 SQL。

禁止 Controller 直接 Mapper。

---

# 72. MyBatis Rule

优先：

```text
Mapper Interface
+
XML
```

处理具有真实业务价值的 SQL。

不得自动切换 MyBatis-Plus。

---

# 73. Python Specific Rule

Python AI Service：

> 保持薄业务层。

不要复制 Java Domain Logic。

Pydantic Schema 用于：

- Request；
- Structured AI Output；
- Response validation。

---

# 74. Vue Specific Rule

Vue UI 首先保证：

> 核心工作流可使用。

优先级：

```text
功能
>
状态反馈
>
易理解
>
视觉精修
```

不要浪费核心开发期构建设计系统。

---

# 75. No Direct Vue → Python Calls

始终：

```text
Vue
↓
Spring Boot
↓
Python
```

禁止绕过 Java Business Backend。

---

# 76. Acceptance Story Must Remain Reproducible

`ACCEPTANCE_TESTS.md` 定义的测试故事和 Seed Facts：

> 不得为了让测试通过而随意修改。

如果测试失败：

> 修实现。

不能：

> 把测试条件改简单。

---

# 77. Baseline vs Memory Experiment

当核心功能稳定后：

必须至少执行一次：

```text
Baseline
vs
Structured Memory
```

实验。

Agent 不得因为 Memory 版本“看起来不错”而跳过 Baseline。

---

# 78. Honest Results Rule

如果实验结果显示：

> Memory 没有明显提升。

必须真实记录。

不得：

- 删除失败记录；
- 修改指标；
- 只展示成功 Case；
- 宣称假设已被证明。

失败实验也是项目结果。

---

# 79. v0.1 Completion Rule

当 `MVP_SCOPE.md` 和 `ACCEPTANCE_TESTS.md` 定义的核心完成条件满足后：

> **停止继续向 v0.1 添加功能。**

执行：

```text
Verification
↓
Acceptance
↓
Known Issues
↓
v0.1 Complete
```

然后再决定 v0.2。

---

# 80. Final Agent Checklist

每次准备把 Task 标记 DONE 前，Agent 必须确认：

```text
[ ] 我实现的是当前 Task，而不是擅自扩大 Scope。

[ ] 我没有引入未经允许的核心技术。

[ ] 我遵守了 Java / Python / MySQL 的职责边界。

[ ] 我检查过已有实现，没有制造重复抽象。

[ ] 我运行了与本次修改相关的测试。

[ ] 测试结果是真实运行结果。

[ ] 我没有隐藏失败。

[ ] 如果修改涉及 AI 输出，我验证了 Structured Output。

[ ] 如果修改涉及业务状态，我确认 MySQL 是最终持久化来源。

[ ] 如果修改涉及 Memory，我保留了必要 Source Evidence。

[ ] .agent/STATE.md 已经能够让新的 Agent 理解现在做到哪里。

[ ] .agent/TASKS.md 状态已经更新。

[ ] 当前仓库处于可以安全继续开发的状态。
```

---

# 81. Final Principle

所有 Coding Agent 最终遵循三个原则：

> **Do the smallest thing that proves the current requirement.**

> **Never expand the product without evidence.**

> **Leave the repository in a state another Agent can understand and continue.**

对于本项目架构，则始终记住：

> **AI proposes. Java decides. MySQL remembers.**