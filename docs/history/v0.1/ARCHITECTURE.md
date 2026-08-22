# ARCHITECTURE.md

## 1. Document Purpose

本文档定义 **AI Story Co-Author v0.1** 的软件架构。

它回答：

- Vue、Spring Boot、Python AI Service、MySQL 如何协作；
- 哪个系统拥有业务状态；
- Planner、Writer、Memory Extraction、Story Query 如何组织；
- Story、Stage、Chapter、Memory 等核心领域对象如何划分；
- Continuous / Step-by-Step Generation 如何运行；
- AI 调用失败或开发中断时如何恢复；
- 哪些架构边界在 v0.1 中必须保持稳定。

本文档不定义：

- 完整 SQL DDL；
- 所有 API 字段；
- Java 类的最终名称；
- Mapper XML 的具体 SQL；
- Prompt 最终文本；
- Vue 页面视觉设计；
- 完整部署架构。

这些由实现阶段在本文档边界内决定。

---

# 2. Architecture Goal

v0.1 架构首先服务：

> **在 7 天范围内完成可运行、可验证、可解释的核心 Story → Plan → Write → Memory → Continue 闭环。**

架构优先级：

```text
简单
>
明确职责
>
数据可靠
>
容易调试
>
容易恢复
>
未来扩展能力
```

禁止为了未来可能出现的问题提前建设复杂基础设施。

---

# 3. System Overview

v0.1 采用四个主要运行组件：

```text
┌──────────────────────────────┐
│          Vue 3 Web           │
│                              │
│ 用户交互 / 阅读 / 控制        │
└───────────────┬──────────────┘
                │ HTTP / JSON
                ▼
┌──────────────────────────────┐
│      Spring Boot Backend     │
│                              │
│ 核心业务系统                  │
│ Story / Stage / Chapter      │
│ Memory / State               │
│ Generation Workflow          │
└──────────┬───────────┬───────┘
           │           │
           │           │ HTTP / JSON
           │           ▼
           │   ┌──────────────────────┐
           │   │   Python AI Service  │
           │   │                      │
           │   │ FastAPI              │
           │   │ LangChain            │
           │   │ Planner              │
           │   │ Writer               │
           │   │ Memory Extraction    │
           │   │ Story Query          │
           │   └───────────┬──────────┘
           │               │
           │               ▼
           │             LLM API
           │
           ▼
┌──────────────────────────────┐
│            MySQL             │
│                              │
│ Persistent Business State    │
└──────────────────────────────┘
```

---

# 4. Core Ownership Rule

整个架构最重要的原则：

> **Spring Boot owns business state.**

> **Python owns AI computation.**

也就是说：

### Spring Boot 决定

- 当前 Story 是什么；
- 当前 Stage 是什么；
- 哪些 Chapter 已经完成；
- 哪些 Memory 生效；
- 当前人物状态是什么；
- 哪些 Candidate 等待 Review；
- Generation Job 处于什么状态。

### Python 决定

- AI 建议如何规划；
- 章节正文如何生成；
- 一章中有哪些候选 Memory；
- 下一步可以有哪些剧情方向；
- 如何基于传入上下文回答 Story Query。

Python 返回：

> **建议 / AI Result**

Spring Boot 决定：

> **如何将 Result 转换成业务状态。**

---

# 5. Single Source of Business Truth

v0.1 中：

> **MySQL 是业务状态唯一持久化来源。**

以下信息不得只存在于：

- Vue State；
- Python 内存；
- LangChain Memory；
- LLM Context；
- Prompt；
- 临时进程。

例如：

```text
Current Location = 冒险者公会
```

如果它已经成为正式 Current State：

> 必须保存到 MySQL。

---

# 6. LLM Is Not a Database

系统不得假设：

> 模型“记得”之前发生过什么。

每次需要 LLM 工作时：

Spring Boot 根据数据库中的 Story State 构造必要上下文，并发送给 Python AI Service。

因此：

```text
Database
→ Context
→ LLM
```

而不是：

```text
旧聊天记录
→ 希望模型自己记得
```

---

# 7. Main Domain Model

v0.1 只保留以下核心领域概念：

```text
Story
Stage
ChapterPlan
Chapter
StoryConstraint
CurrentState
RelationshipState
MemoryCandidate
StoryMemory
GenerationJob
```

不增加：

```text
Canon Authority
Temporal Truth Graph
Knowledge Graph Node
Memory Projection Entity
Agent Conversation Entity
```

除非真实开发证明必要。

---

# 8. Story

Story 表示一个独立小说项目。

它是大多数业务对象的顶层 Scope。

概念关系：

```text
Story
├── Constraints
├── Stages
├── Chapters
├── Current States
├── Relationships
├── Story Memories
└── Generation Jobs
```

不同 Story 的数据必须隔离。

---

# 9. StoryConstraint

StoryConstraint 表示作者希望长期保持的高优先级创作规则。

例如：

```text
TYPE: STYLE
CONTENT: 使用轻松爽文式风格

TYPE: PERSPECTIVE
CONTENT: 第三人称限知

TYPE: WORLD_RULE
CONTENT: 普通人认为力量来源于魔力
```

v0.1 不要求复杂层级。

关键目标：

> Writer 和 Planner 能稳定获取作者确认的 Story Constraints。

---

# 10. Stage

Stage 表示：

> 作者当前希望推进的一段剧情方向。

例如：

```text
前往冒险者公会注册，并第一次展示特殊力量。
```

建议状态：

```text
DRAFT
PLANNED
RUNNING
PAUSED
COMPLETED
FAILED
```

具体 Enum 名称允许实现阶段调整。

---

# 11. ChapterPlan

Stage Planning 后产生多个 ChapterPlan。

例如：

```text
Stage

├── ChapterPlan 1
│   到达冒险者公会
│
├── ChapterPlan 2
│   完成注册测试
│
└── ChapterPlan 3
    突发事件中展示力量
```

ChapterPlan 的主要职责：

> 限制 Writer 当前到底应该完成什么。

它不是正文。

---

# 12. Chapter

Chapter 保存：

> 小说真正生成出来的正文。

Chapter 是原始创作内容。

至少逻辑上包含：

```text
Story
Chapter Number
Title
Content
Summary
Generation Status
```

正文必须完整保存。

不得只保存 Summary。

---

# 13. Chapter as Source Evidence

重要架构原则：

> **Chapter 是原始内容，Memory 是 AI 对 Chapter 的理解。**

因此：

```text
Chapter
≠
Memory
```

Memory Agent 出错时：

> Chapter 仍然保留真实正文。

---

# 14. CurrentState

CurrentState 用于回答：

> 某个对象“现在”是什么状态？

v0.1 优先支持角色状态。

例如：

```text
Character: 林凡

LOCATION
冒险者公会

EMOTION
警惕

PHYSICAL_CONDITION
右肩受伤

CURRENT_GOAL
完成冒险者注册
```

---

# 15. Current State Model

v0.1 采用：

> **Current Value First**

也就是当前状态只需要可靠表达：

```text
现在是什么？
```

例如：

```text
weapon = wooden_sword
```

后来木剑损坏：

```text
weapon = none
```

v0.1 不要求同时实现完整：

```text
valid_from
valid_to
historical truth
temporal query engine
```

历史变化仍可从 Chapter / StoryMemory 中查找。

如果真实实验证明 Current Value 不够，再升级 Temporal Model。

---

# 16. RelationshipState

人物关系单独视为一种当前状态。

概念：

```text
Character A
Character B
Relationship Description
Source
```

例如：

```text
艾琳 → 林凡

Helpful but still suspicious
```

v0.1 不强制将关系转换成：

```text
-100 ~ +100
```

这样的数值模型。

自然语言描述优先。

---

# 17. MemoryCandidate

MemoryCandidate 是：

> Memory Agent 从新 Chapter 中提取出来、但尚未全部正式应用的信息。

Candidate 至少需要表达：

```text
Story
Source Chapter
Type
Subject
Description / Value
Suggested Action
Evidence
Processing Status
```

Suggested Action：

```text
AUTO
REVIEW
IGNORE
```

---

# 18. StoryMemory

StoryMemory 保存：

> 已经被系统或作者决定长期保留的信息。

主要用于：

- Writer Context；
- Planner；
- Story Query。

优先保存：

```text
重要事件
重要细节
潜在 / 已确认伏笔
人物秘密
重要承诺
异常现象
```

---

# 19. Candidate → Memory Flow

流程：

```text
Chapter
↓
Memory Extraction
↓
MemoryCandidate
↓
AUTO / REVIEW / IGNORE
```

### AUTO

Spring Boot 可以：

```text
创建 / 更新正式 Memory
或
更新 Current State
```

### REVIEW

保存为 Pending Candidate。

等待作者决定。

### IGNORE

Candidate 可以保留审查记录，但默认不进入有效 StoryMemory。

为了 v0.1 简化：

> 如果保存全部 IGNORE Candidate 明显增加复杂度，也允许直接标记为 IGNORED，而不是建设复杂历史系统。

---

# 20. Memory Evidence

Memory 应尽可能保留：

```text
source_chapter_id
evidence_text
```

例如：

```text
Memory:
艾琳对主角敌意降低

Source:
Chapter 3

Evidence:
“她虽然仍然抱怨，却没有再要求他离开。”
```

v0.1 不要求保存精确字符 Offset。

未来 Chunk / Vector Retrieval 加入时再考虑：

```text
source_chunk_id
```

---

# 21. Spring Boot Internal Architecture

主后端推荐采用：

> **按业务模块组织 + 模块内部经典分层。**

不是纯技术目录全部堆在一起。

推荐概念：

```text
story
stage
chapter
memory
generation
ai
```

每个模块内部根据需要包含：

```text
controller
service
mapper
model
dto
```

---

# 22. Recommended Java Package Direction

示意：

```text
com.example.storyai

├── story
│   ├── controller
│   ├── service
│   ├── mapper
│   ├── model
│   └── dto
│
├── stage
│
├── chapter
│
├── memory
│
├── generation
│
├── ai
│   ├── client
│   └── dto
│
├── common
│   ├── exception
│   ├── response
│   └── config
│
└── Application
```

实际实现可以调整。

核心原则：

> 业务模块必须清楚。

不要发展成：

```text
controller/
service/
mapper/
entity/
```

下面各自塞几十个毫无领域聚合关系的类。

---

# 23. Controller Responsibility

Controller 只负责：

```text
接收 HTTP Request
↓
参数校验
↓
调用 Service
↓
返回 Response
```

Controller 不应该：

- 编写复杂业务规则；
- 调用 MyBatis Mapper 执行业务流程；
- 构造完整 Prompt；
- 直接调用 LLM Provider。

---

# 24. Service Responsibility

Service 是 Spring Boot 核心业务层。

例如：

```text
StoryService
StageService
ChapterService
MemoryService
GenerationService
```

其中 `GenerationService` 很重要。

它负责组织：

```text
读取业务状态
↓
构造 AI Request
↓
调用 Python
↓
验证 AI Response
↓
持久化结果
↓
推进 Generation Job
```

---

# 25. Mapper Responsibility

MyBatis Mapper 只负责：

> 数据库读写。

不得承担业务决策。

例如 Mapper 可以：

```text
selectActiveMemoriesByStoryId()
```

但不应该决定：

> 哪些 Memory 应该发送给 Writer。

这属于 Service / Context Assembly。

---

# 26. AI Client

Spring Boot 内建立统一 AI Service Client。

概念：

```text
AiServiceClient
```

负责调用：

```text
/plan-stage
/generate-chapter
/extract-memory
/suggest-directions
/story-query
```

业务 Service 不应该在各处重复手写 HTTP 调用。

---

# 27. Python AI Service Architecture

Python AI Service 保持简单。

推荐概念：

```text
app/

├── api/
├── services/
│   ├── planner.py
│   ├── writer.py
│   ├── memory_extractor.py
│   └── story_query.py
│
├── schemas/
├── prompts/
├── llm/
└── main.py
```

它不需要复制 Java 的完整业务 Domain。

---

# 28. Python Service Principle

每个 AI 能力尽量表现为：

```text
Structured Input
↓
Prompt / LangChain
↓
LLM
↓
Structured Output
```

例如：

```text
MemoryExtractionRequest
↓
MemoryExtractor
↓
MemoryExtractionResponse
```

而不是建立：

> 一个无边界万能 Agent。

---

# 29. Planner Architecture

Planner 有两个操作：

### Plan Stage

```text
Stage Direction
+
Story Context
↓
Suggested Chapter Count
+
Chapter Plans
```

### Suggest Directions

```text
Current Story Context
↓
3 Candidate Directions
```

Planner 不直接修改 MySQL。

---

# 30. Writer Architecture

Writer 输入：

```text
Story Constraints
Current Stage
Current Chapter Plan
Current State
Relevant Story Memory
Recent Story Context
```

输出：

```text
Title
Content
Summary
```

Writer 不负责：

- 保存 Chapter；
- 修改 Current State；
- 接受 Memory；
- 修改 Stage。

---

# 31. Memory Extractor Architecture

Memory Extractor 输入：

```text
New Chapter
+
Existing Relevant State
+
Story Constraints
```

主要目的是避免它把已有信息误认为全部是新信息。

输出：

```text
Memory Candidates[]
```

每个 Candidate 包含：

```text
type
subject
field / description
value
suggestedAction
evidence
```

具体 Schema 可在实现阶段精简。

---

# 32. Story Query Architecture

v0.1 不建设自主搜索 Agent。

Spring Boot 首先根据问题提供：

```text
Current State
Relevant Memory
Known Evidence
Recent Summary
```

Python Query Service 根据这些信息生成自然语言回答。

对于明确结构化问题：

例如：

> 主角当前在哪里？

未来可以直接由 Java 查询回答。

但 v0.1 允许统一通过 Story Query Service 输出自然语言。

---

# 33. Story Query Truth Boundary

Query Service 必须被明确要求：

> 只根据传入的 Story Context 回答。

如果找不到：

```text
Unknown
```

而不是自由补全。

---

# 34. Context Assembly

Context Assembly 是 Spring Boot 中非常重要的职责。

它负责：

> 从 MySQL 选择当前 AI 任务需要的信息。

v0.1 使用简单策略。

例如 Writer Context：

```text
All Story Constraints

+

Current Chapter Plan

+

Relevant Current States

+

Active Story Memories

+

Previous Chapter Summary
```

---

# 35. v0.1 Retrieval Strategy

v0.1 暂时不实现复杂相关性检索。

如果 Story Memory 数量较少：

> 可以直接按 Story、Type、Subject 等结构化条件查询。

例如：

```text
当前章节涉及：
林凡、艾琳

→ 查询两人的 Current State
→ 查询两人相关 Story Memory
```

必要时甚至允许 v0.1 先传入所有少量 Story Memory。

但必须记录：

> 当 Memory 数量增长后，此策略需要重新评估。

---

# 36. Recent Context

Writer 除 Structured Memory 外还需要近期叙事连续性。

v0.1 推荐优先提供：

```text
上一章 Summary
```

必要时增加：

```text
上一章结尾有限原文
```

不要默认把全部历史 Chapter 塞进 Prompt。

---

# 37. GenerationJob

为了实现 Continuous Mode，同时避免引入 MQ，v0.1 使用简单：

> **Generation Job**

概念状态：

```text
PENDING
RUNNING
PAUSED
COMPLETED
FAILED
```

Generation Job 至少知道：

```text
Story
Stage
Generation Mode
Current Chapter Index
Total Planned Chapters
Status
Last Error
```

---

# 38. Step-by-Step Workflow

流程：

```text
Author clicks Generate
↓
Spring Boot creates / resumes Job
↓
Load next ChapterPlan
↓
Build Writer Context
↓
Call Python Writer
↓
Save Chapter
↓
Call Memory Extractor
↓
Process AUTO
↓
Save REVIEW
↓
Update Current State
↓
Job → PAUSED
```

作者点击：

```text
Continue
```

再执行下一章。

---

# 39. Continuous Workflow

Continuous Mode 逻辑上重复相同的单章事务：

```text
for each remaining ChapterPlan:

    Generate Chapter

    Persist Chapter

    Extract Memory

    Apply Safe Updates

    Persist Candidates

    Advance Job
```

重点：

> Continuous Mode 不是另一套系统。

它只是自动连续调用同一个：

> `Generate One Chapter`

流程。

---

# 40. One Chapter = One Recovery Boundary

这是 v0.1 很重要的可靠性原则：

> **一章作为一个最小稳定恢复点。**

理想流程：

```text
生成 Chapter N
↓
保存
↓
Memory Extraction
↓
状态更新
↓
Job checkpoint
↓
开始 Chapter N+1
```

这样即使：

```text
Chapter N+1 AI 调用失败
```

Chapter N 仍然安全存在。

---

# 41. Transaction Boundary

不能把：

> LLM 网络调用

放进一个长时间数据库事务里。

错误方式：

```text
BEGIN TRANSACTION

Call LLM for 60 seconds

Insert Chapter

...

COMMIT
```

v0.1 应避免这种设计。

---

# 42. Recommended Persistence Flow

例如生成章节：

```text
读取数据库状态
↓
提交 AI Request
↓
等待 AI Result
↓
开始短数据库事务
↓
保存 Chapter / 更新 Job
↓
提交事务
```

Memory Update 同理。

---

# 43. Chapter Generation Failure

如果 Writer 调用失败：

```text
Job = FAILED
或 PAUSED_WITH_ERROR
```

不得创建：

```text
Completed Empty Chapter
```

Retry 时：

> 再次执行当前 ChapterPlan。

---

# 44. Memory Extraction Failure

如果：

```text
Chapter 已经保存
```

但：

```text
Memory Extraction 失败
```

那么：

```text
Chapter 保留
Memory Status = EXTRACTION_FAILED
Job 暂停
```

用户或系统可以：

> Retry Memory Extraction。

在 Extraction 成功之前：

> Continuous Mode 不应该直接继续下一章。

原因：

否则下一章可能缺失新状态。

---

# 45. Idempotency Principle

Retry 不应该无限创建重复业务数据。

例如：

第一次：

```text
Chapter 3 已经成功保存
```

随后客户端超时。

Retry 不应该生成：

```text
Chapter 3
Chapter 3 Duplicate
```

因此 ChapterPlan → Chapter 应有明确对应关系。

同一个 ChapterPlan：

> 最终只能存在一个当前有效生成结果。

具体数据库约束由实现阶段决定。

---

# 46. Memory Update Safety

AUTO Candidate 可以自动更新。

但 Spring Boot 必须控制：

> 哪些 Candidate 类型有资格直接改变 Current State。

例如：

```text
LOCATION
INVENTORY
PHYSICAL_STATE
```

可以根据明确规则应用。

不能简单：

```text
if AUTO:
    任意写入数据库
```

---

# 47. REVIEW Isolation

REVIEW Candidate：

> 不得在作者确认前悄悄成为正式世界规则或核心 StoryMemory。

但是：

> 它仍然存在于 Review Queue。

v0.1 Continuous Mode 默认允许非阻塞 REVIEW 等待阶段结束统一处理。

如果实际测试证明某一 Candidate 不确认就无法继续：

> 记录为 v0.2 的 Blocking Review 候选能力。

---

# 48. Frontend Architecture

Vue 前端保持普通 SPA 架构。

推荐概念：

```text
src/

├── views/
├── components/
├── api/
├── stores/
├── router/
├── types/
└── utils/
```

---

# 49. Frontend State Rule

Pinia 主要负责：

- 当前 Story ID；
- 当前 Workspace 必要状态；
- Generation UI Status；
- 少量共享状态。

后端数据库仍然是业务状态来源。

刷新页面后：

> 应重新从 Spring Boot 获取正式数据。

---

# 50. API Flow

用户请求统一：

```text
Vue
↓
Spring Boot
```

Spring Boot 根据需要：

```text
Spring Boot
↓
Python AI Service
```

禁止正常产品 UI：

```text
Vue
↓
Python
```

原因：

> 用户业务权限和状态必须统一经过 Java Backend。

---

# 51. API Design Style

v0.1 使用：

> REST-style JSON API。

不引入 GraphQL。

示意：

```text
/stories

/stories/{storyId}

/stories/{storyId}/stages

/stages/{stageId}/plan

/stages/{stageId}/generation

/stories/{storyId}/chapters

/stories/{storyId}/memories

/stories/{storyId}/memory-candidates

/stories/{storyId}/query
```

具体 URL 可以在实现时调整。

---

# 52. DTO Boundary

不得直接把数据库 Entity 作为所有 API Response。

至少逻辑上区分：

```text
Database Model

Request DTO

Response DTO

AI Service DTO
```

但 v0.1 不为了纯形式制造几十个无意义 DTO。

原则：

> 当两个边界语义不同，就创建 DTO。

---

# 53. AI API Contract

Java ↔ Python 必须尽量使用稳定结构化 JSON。

Python 不应该返回：

```text
"Here is your result: ..."
```

让 Java 再手动 Regex。

对于：

- Plan；
- Memory Extraction；
- Planner Suggestion；

优先 Structured Output。

---

# 54. Error Model

Spring Boot 对前端至少区分：

```text
Validation Error

Business Error

AI Service Error

Generation Error

Internal Error
```

不用第一版建设复杂 Error Code Platform。

但必须避免所有错误都变成：

```text
500 Unknown Error
```

---

# 55. Logging

关键链路日志建议带：

```text
storyId
stageId
chapterPlanId
chapterId
generationJobId
```

这样 Agent / 开发者调试时可以追踪：

```text
用户点击生成
↓
哪个 Stage
↓
哪个 Chapter
↓
哪个 AI 调用
↓
哪里失败
```

---

# 56. Security Boundary

v0.1 是单用户项目，不建设复杂认证。

但是必须遵守：

- API Key 不进入 Git；
- 数据库密码不写死；
- Python AI Service 不向浏览器暴露 Provider Key；
- 前端只能调用 Spring Boot；
- 日志不得输出真实 API Key。

---

# 57. Development Recovery

Vibecoding 开发中断与产品运行恢复是两件不同的事情。

### Product Recovery

依赖：

```text
MySQL GenerationJob
+
Stage / Chapter 状态
```

### Development Recovery

依赖：

```text
Git
+
AGENTS.md
+
.agent/STATE.md
+
.agent/TASKS.md
```

不得把开发 Agent 状态存入产品数据库。

---

# 58. Architecture Validation Rule

每增加一个组件，必须回答：

> 如果删除这个组件，v0.1 核心需求还能否实现？

如果答案：

> 可以。

则默认不加入。

---

# 59. Technologies Explicitly Not Present

v0.1 架构中没有：

```text
Redis

Kafka

RabbitMQ

RocketMQ

Spring Cloud

Service Registry

API Gateway

Elasticsearch

Vector Database

Knowledge Graph

LangGraph

Kubernetes

Distributed Transaction
```

Coding Agent 不得自行增加。

---

# 60. Future RAG Extension Point

如果后续实验证明需要历史原文语义检索：

架构允许新增：

```text
Chapter
↓
ChapterChunk
↓
Embedding
↓
Vector Retrieval
```

并保持：

```text
Chunk
→ chapter_id
→ original Chapter
```

但是：

> Vector Index 只是检索索引。

Chapter 仍然是原始正文来源。

Structured Memory 仍然独立存在。

---

# 61. Future Temporal Extension Point

如果测试出现：

> Current Value 无法回答历史状态和状态变化原因。

可以扩展：

```text
CurrentState
↓
StateHistory / StateTransition
```

而不是推翻 Story / Chapter / Memory 整体模型。

---

# 62. Future Workflow Extension Point

如果后续出现：

- 大量分支；
- 多次 Human Review；
- 可恢复 AI 循环；
- Agent 互相调用；
- 长时间 workflow；

才重新评估：

> LangGraph / Workflow Engine。

v0.1 普通 Service Orchestration 优先。

---

# 63. Architecture Success Criteria

v0.1 架构成功不意味着：

> 架构非常先进。

而意味着：

1. Vue 可以完成完整用户流程；
2. Spring Boot 拥有清晰业务状态；
3. MySQL 能可靠恢复 Story 状态；
4. Python AI Service 不污染业务数据库；
5. Writer、Planner、Memory、Query 职责可解释；
6. Continuous Mode 不需要 MQ 也能正确运行；
7. 一章失败不会破坏之前章节；
8. Memory Extraction 失败能够独立 Retry；
9. 后续真实问题出现时仍有合理扩展路径；
10. 开发者能够从头解释一次完整请求的数据流。

---

# 64. End-to-End Data Flow

完整核心流程：

```text
AUTHOR
  │
  │ 输入 Stage Direction
  ▼
VUE
  │
  ▼
SPRING BOOT
  │
  │ 读取 Story / Memory / State
  ▼
PYTHON PLANNER
  │
  │ 返回 Chapter Plans
  ▼
SPRING BOOT
  │
  │ 保存 Plan
  ▼
AUTHOR CONFIRM
  │
  ▼
GENERATION JOB
  │
  ▼
SPRING BOOT
  │
  │ 构造 Writer Context
  ▼
PYTHON WRITER
  │
  │ 返回 Chapter
  ▼
SPRING BOOT
  │
  │ 保存 Chapter
  ▼
PYTHON MEMORY EXTRACTOR
  │
  │ 返回 Candidates
  ▼
SPRING BOOT
  │
  ├── AUTO → Apply State / Memory
  │
  ├── REVIEW → Pending Review
  │
  └── IGNORE → Ignore
  │
  ▼
MYSQL CHECKPOINT
  │
  ▼
NEXT CHAPTER
```

---

# 65. Architecture Final Principle

v0.1 架构遵循：

> **AI proposes. Java decides. MySQL remembers.**

对应中文：

> **AI 提出结果，Java 决定业务状态，MySQL 负责可靠保存。**

同时：

> **作者决定方向，系统负责执行。**

任何后续架构修改都必须继续服务于这两个原则，而不是为了增加系统复杂度。