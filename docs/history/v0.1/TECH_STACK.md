# TECH_STACK.md

## 1. Document Purpose

本文档定义 **AI Story Co-Author v0.1** 的技术栈、技术职责边界与技术引入规则。

它回答：

- v0.1 使用哪些主要技术；
- Java、Python、Vue 分别负责什么；
- 为什么选择这些技术；
- 哪些技术当前明确不使用；
- 新技术在什么情况下才允许加入。

本文档的技术选择同时服务两个目标：

1. **完成 AI Story Co-Author v0.1 的真实产品验证；**
2. **让项目技术栈与开发者当前 Java 后端求职方向和已有学习经历保持一致。**

禁止为了展示更多技术关键词而无理由扩大技术栈。

---

# 2. Core Technology Principle

v0.1 遵循：

> **Use familiar technologies for core engineering, and introduce new technologies only when the product genuinely requires them.**

技术选型优先级：

```text
能够完成核心闭环
>
开发者能够理解和解释
>
稳定与可维护
>
开发效率
>
技术新颖程度
```

项目不追求：

> 使用最多的框架。

而追求：

> 每一个出现在简历中的技术，都承担真实职责，并且开发者能够解释为什么使用它。

---

# 3. System Technology Overview

v0.1 采用：

```text
┌─────────────────────────────┐
│          Vue 3 Web          │
│                             │
│ Story / Chapter / Memory UI │
└──────────────┬──────────────┘
               │
            HTTP / JSON
               │
               ▼
┌─────────────────────────────┐
│       Spring Boot Backend   │
│                             │
│ Business Logic              │
│ Story State                 │
│ Memory Management           │
│ Stage / Chapter Management  │
│ REST API                    │
│ MyBatis                     │
└───────┬──────────────┬──────┘
        │              │
        │              │ HTTP / JSON
        │              ▼
        │     ┌─────────────────────┐
        │     │ Python AI Service   │
        │     │                     │
        │     │ FastAPI             │
        │     │ LangChain           │
        │     │ Planner             │
        │     │ Writer              │
        │     │ Memory Extraction   │
        │     │ Story Query         │
        │     └──────────┬──────────┘
        │                │
        │                ▼
        │             LLM API
        │
        ▼
┌─────────────────────────────┐
│            MySQL            │
│                             │
│ Story                       │
│ Chapter                     │
│ Stage                       │
│ Memory                      │
│ Current State               │
│ Generation State            │
└─────────────────────────────┘
```

核心原则：

> **Java owns business state. Python owns AI computation.**

即：

> Java 负责业务事实和状态，Python 负责 AI 推理和生成。

---

# 4. Java Runtime

## 4.1 Java Version

v0.1 使用：

> **Java 21 LTS**

原因：

- 属于长期支持版本；
- 适合现代 Spring Boot 3.x；
- 能够作为当前 Java 后端项目的稳定运行基础；
- 同时避免为了最新 Java 版本增加没有实际价值的学习成本。

v0.1 不要求为了展示新语法强制使用 Java 21 的高级特性。

优先编写：

> 简单、清晰、符合常见 Java 后端工程习惯的代码。

---

# 5. Main Backend

## 5.1 Spring Boot

主业务后端使用：

> **Spring Boot 3.5.x**

不使用 Spring Boot 4.x 作为 v0.1 默认版本。

原因：

- Spring Boot 3.x 仍属于稳定维护版本；
- 更符合当前开发者已有学习路径；
- Java 后端相关资料、生态和实际项目经验更加容易复用；
- v0.1 不需要 Spring Boot 4 才能提供的能力。

具体 patch 版本应在项目初始化时选择当前稳定的 3.5.x 版本，并通过 Maven 锁定。

---

## 5.2 Spring Boot Responsibilities

Spring Boot 是：

> **整个产品的主业务系统。**

它必须承担真实业务职责，而不是只做 Python AI Service 的转发层。

至少负责：

### Story Management

- 创建 Story；
- 查询 Story；
- 修改基础信息；
- 保存 Story Constraints。

### Stage Management

- 创建 Stage Direction；
- 保存 Stage Plan；
- 修改 Stage Plan；
- 管理 Stage 状态。

### Chapter Management

- 保存 Chapter；
- 查询 Chapter；
- 管理 Chapter 顺序；
- 保存 Chapter Summary；
- 管理生成状态。

### Memory Management

- 保存 Memory Candidate；
- 应用 AUTO Memory；
- 管理 REVIEW Queue；
- 接受 / 修改 / 忽略 Memory；
- 查询 Story Memory。

### Current State

- 维护当前人物状态；
- 更新当前位置；
- 更新物品；
- 更新身体 / 情绪状态；
- 更新人物关系。

### Generation Workflow

负责控制：

```text
Plan
→
Generate
→
Extract Memory
→
Persist
→
Continue
```

而不是让 Python AI Service 自己决定业务状态。

### REST API

向 Vue 前端提供统一业务 API。

### AI Service Integration

负责调用 Python AI Service，并处理：

- 请求 DTO；
- 响应 DTO；
- 超时；
- 调用失败；
- 非法返回结果。

---

# 6. Spring Modules

v0.1 优先使用：

### Spring Web

负责 REST API。

### Spring Validation

负责请求参数校验。

### Spring Transaction

用于需要原子性的数据库业务操作。

例如：

```text
Memory Review
+
Current State Update
```

在需要时可以放在明确事务边界中。

### Spring Boot Configuration

管理：

- 数据库配置；
- AI Service URL；
- 环境变量；
- 外部配置。

---

# 7. Persistence Layer

## 7.1 MyBatis

数据库访问使用：

> **MyBatis 3**

Spring Boot 集成使用：

> **MyBatis Spring Boot Starter 3.x**

不得在 v0.1 中自动替换为：

- JPA；
- Hibernate；
- MyBatis-Plus。

---

## 7.2 Why MyBatis

选择 MyBatis 的主要原因：

### Reason 1 — Developer Learning Alignment

开发者当前重点学习：

> Java + Spring Boot + MyBatis + MySQL。

项目应提供真实练习机会。

### Reason 2 — SQL Visibility

Story Memory 项目存在较多明确业务查询。

例如：

```text
查询某个 Story 的所有未处理 Memory Candidate

查询某角色当前状态

查询当前未解决伏笔

查询某个 Chapter 的 Memory Evidence
```

使用 MyBatis 能够让 SQL 行为保持清晰可见。

### Reason 3 — Interview Value

开发者应该能够通过本项目真正理解：

- Mapper；
- XML Mapping；
- `resultMap`；
- 参数绑定；
- 动态 SQL；
- JOIN；
- 索引；
- 分页；
- SQL 查询优化；
- 事务与数据库操作之间的关系。

---

# 8. MyBatis Usage Style

v0.1 推荐：

> **Mapper Interface + XML SQL 为主要方式。**

简单 SQL 可以使用 Annotation。

但不应把所有复杂 SQL 塞进 Java Annotation。

推荐：

```text
Mapper Interface
        ↓
Mapper XML
        ↓
MySQL
```

---

## 8.1 SQL Principle

SQL 应保持：

> 简单、明确、符合业务需求。

不得为了展示 MyBatis 而故意编写复杂 SQL。

例如：

简单按主键查询：

```sql
SELECT ...
FROM story
WHERE id = ?
```

保持简单。

只有真实业务需要时再使用：

- JOIN；
- 动态条件；
- 聚合；
- 分页；
- 批量操作。

---

# 9. Database

## 9.1 MySQL

v0.1 使用：

> **MySQL 8.4 LTS**

MySQL 是产品业务状态的主要持久化数据库。

---

## 9.2 MySQL Responsibilities

至少用于持久化：

```text
Story

Story Constraints

Stage

Stage Plan

Chapter

Chapter Summary

Memory Candidate

Story Memory

Current State

Relationship State

Generation Job / Status
```

具体 Schema 由 `ARCHITECTURE.md` 和实现阶段决定。

---

## 9.3 Why MySQL

主要原因：

### Developer Alignment

MySQL 是当前开发者 Java 后端学习的重要数据库。

### Product Requirement

v0.1 数据主要属于：

> 结构化、关系明确、需要可靠持久化的业务数据。

MySQL 足以满足当前需求。

### Interview Preparation

项目可以产生真实的：

- 表结构设计；
- 主键设计；
- 外键 / 逻辑关联设计；
- 唯一索引；
- 普通索引；
- JOIN 查询；
- 状态查询；
- 事务；
- 数据一致性；

等 Java 后端面试讨论内容。

---

# 10. Database Ownership

必须遵守：

> **Spring Boot owns MySQL business data.**

Python AI Service 在 v0.1 中：

> **不得直接修改核心业务数据库。**

禁止出现：

```text
Spring Boot
→ 修改 Memory

同时

Python
→ 直接修改同一张 Memory 表
```

否则会产生两个业务事实来源。

正确流程：

```text
Python AI Service
        ↓
返回 AI 结果
        ↓
Spring Boot
        ↓
验证 / 转换
        ↓
MySQL
```

---

# 11. Python Runtime

v0.1 使用：

> **Python 3.12+ 稳定版本**

优先选择开发机器、LangChain 和 Provider SDK 能稳定支持的 Python 版本。

不要求为了追求最新版 Python 而升级。

---

# 12. Python AI Service

Python 负责：

> **AI computation layer**

而不是业务系统。

它主要处理：

- Prompt Construction；
- LLM Call；
- Structured Output；
- Stage Planning；
- Chapter Writing；
- Memory Extraction；
- Planner Suggestions；
- Story Query Reasoning。

---

# 13. FastAPI

Python 服务使用：

> **FastAPI**

FastAPI 的职责非常有限：

> 将 Python AI 能力暴露为 HTTP API 给 Spring Boot。

例如概念接口：

```text
POST /ai/plan-stage

POST /ai/replan-stage

POST /ai/generate-chapter

POST /ai/extract-memory

POST /ai/suggest-directions

POST /ai/story-query
```

具体接口名称和 Schema 由 Architecture 定义。

---

## 13.1 Why FastAPI

主要原因：

- 与 Python 类型系统和 Pydantic 配合自然；
- 快速构建 JSON API；
- 适合作为轻量 AI Service；
- 不要求开发者学习复杂 Python Web Framework。

FastAPI 不是项目的主后端。

---

# 14. LangChain

AI 编排使用：

> **LangChain Python**

v0.1 不要求复杂 Agent Framework。

LangChain 主要用于：

### Model Abstraction

统一模型调用方式。

### Prompt Construction

组织：

- Story Constraints；
- Stage Plan；
- Chapter Goal；
- Current State；
- Story Memory；
- Recent Context。

### Structured Output

Memory Extraction、Stage Planning 等结果必须优先使用结构化输出。

例如 Memory Extraction 应产生类似：

```json
{
  "candidates": [
    {
      "type": "CURRENT_STATE",
      "subject": "主角",
      "field": "location",
      "value": "冒险者公会",
      "suggestedAction": "AUTO",
      "evidence": "..."
    }
  ]
}
```

而不是要求 Java 解析：

```text
“我认为本章包含以下几个比较重要的记忆……”
```

---

# 15. LangChain Boundaries

v0.1 不允许因为使用 LangChain 就将所有 AI 调用包装为复杂 Agent。

例如：

```text
Stage Planner
```

可以只是：

```text
Prompt
+
Structured LLM Call
```

并不一定需要：

```text
Autonomous Agent
+
Tools
+
Planning Loop
```

---

# 16. LangGraph

v0.1：

> **不主动使用 LangGraph。**

原因：

当前工作流基本确定：

```text
Plan
→
Write
→
Extract
→
Persist
→
Continue
```

普通应用层控制已经足够。

只有以后真实出现：

- 复杂分支；
- Agent 循环；
- Durable Execution；
- AI Workflow Checkpoint；
- 多级 Human-in-the-loop；
- 复杂状态图；

并且普通代码已经明显难以管理时，才重新评估 LangGraph。

---

# 17. LLM Provider

v0.1 不在技术架构中永久绑定单一 Provider。

AI Service 应通过：

> LangChain Model Interface

尽可能隔离 Provider 调用。

但是：

> **v0.1 只需要实际支持一个 Provider。**

禁止为了所谓“可扩展性”提前实现复杂 Multi-Provider Routing。

模型选择原则：

- 中文能力足够；
- 长文本能力足够；
- Structured Output 相对稳定；
- API 可以可靠调用；
- 成本在测试预算内。

具体 Provider 和 Model 通过环境配置确定。

---

# 18. Frontend

## 18.1 Vue

Web Frontend 使用：

> **Vue 3**

推荐采用 Vue 3 Composition API。

---

## 18.2 Frontend Build Tool

使用：

> **Vite**

不引入额外复杂构建体系。

---

# 19. Frontend Core Libraries

v0.1 推荐：

### Vue Router

负责页面路由。

### Pinia

负责必要的前端共享状态。

只保存真正需要跨组件共享的 UI / application state。

不得把后端业务状态复制成复杂前端状态系统。

### Axios

负责调用 Spring Boot REST API。

---

# 20. Frontend Responsibility

Vue 负责：

- Story 创建；
- Story Workspace；
- Chapter 阅读；
- Stage Direction 输入；
- Plan 展示；
- Chapter Count 调整；
- Generation Mode；
- Generation Progress；
- Memory Panel；
- Memory Review；
- Planner Suggestions；
- Story Query。

Vue 不负责：

- Memory 业务规则；
- AI Prompt；
- Story State 决策；
- 数据库逻辑。

---

# 21. Frontend Language

优先使用：

> **TypeScript**

而不是纯 JavaScript。

原因：

- API DTO 更容易表达；
- 与后端结构化数据交互更清晰；
- 降低复杂 Memory 数据传递中的字段错误。

如果 TypeScript 明显成为 7 天开发阻塞项，可以降级为 JavaScript。

但默认保持 TypeScript。

---

# 22. Frontend UI Library

v0.1：

> **不强制指定大型 UI Framework。**

如果开发 Agent 需要快速实现：

- Form；
- Dialog；
- Table；
- Tabs；
- Drawer；
- Progress；

可以选择一个成熟 Vue 3 UI Component Library。

但必须：

- 只选择一个；
- 不自行开发 Design System；
- 不花大量时间做视觉主题；
- 不让 UI 框架成为项目核心技术卖点。

具体选择可在实现初始化时决定。

---

# 23. Java ↔ Python Communication

Spring Boot 与 Python AI Service 使用：

> **HTTP + JSON**

原因：

- 实现简单；
- 易调试；
- 服务边界清楚；
- 不需要引入 RPC Framework。

---

## 23.1 Example

Spring Boot：

```text
GenerationContext
```

发送给 Python：

```json
{
  "storyConstraints": [],
  "stageDirection": "...",
  "chapterGoal": "...",
  "currentState": [],
  "storyMemories": [],
  "recentContext": []
}
```

Python 返回：

```json
{
  "title": "...",
  "content": "...",
  "summary": "..."
}
```

Spring Boot 负责：

> 是否以及如何把结果写入业务数据库。

---

# 24. API Ownership

前端：

```text
Vue
↓
Spring Boot
```

禁止：

```text
Vue
↓
Python AI Service
```

正常产品请求必须经过 Spring Boot。

原因：

> Spring Boot 是产品业务入口。

Python AI Service 是内部能力服务。

---

# 25. Build Tools

## Java

使用：

> **Maven**

主要负责：

- Java Dependency Management；
- Build；
- Test；
- Packaging。

---

## Frontend

使用：

> **npm**

v0.1 不因为包管理工具偏好引入 pnpm / yarn 迁移成本。

---

## Python

使用标准 Python virtual environment + dependency file。

可以采用：

```text
requirements.txt
```

或：

```text
pyproject.toml
```

优先保证：

> 项目可以被 Coding Agent 和开发者稳定重新安装。

---

# 26. Source Control

使用：

> **Git**

关键开发阶段应提交可运行状态。

推荐：

```text
One completed task
→
Verification
→
Commit
```

Git 同时承担：

- 源码版本管理；
- Agent 中断恢复辅助；
- 技术演进记录。

---

# 27. Environment Configuration

敏感配置不得写入 Git。

至少通过环境变量管理：

```text
MYSQL_HOST

MYSQL_PORT

MYSQL_DATABASE

MYSQL_USERNAME

MYSQL_PASSWORD

AI_SERVICE_URL

LLM_API_KEY

LLM_MODEL
```

仓库提供：

```text
.env.example
```

或等价示例配置。

不得包含真实 API Key。

---

# 28. Testing Stack

## Java

优先使用：

- JUnit 5；
- Spring Boot Test；
- 必要的 Mock 工具。

至少覆盖核心 Service 与业务规则。

---

## Python

使用：

> pytest

优先测试：

- Structured Output Parsing；
- Prompt Input Assembly；
- AI Service API Contract；
- 非 LLM 纯逻辑。

真实 LLM 行为主要通过 Acceptance Tests 验证。

---

## Frontend

v0.1 不强制建设完整前端测试体系。

核心时间优先投入：

> Backend + AI + End-to-End Product Loop。

---

# 29. API Documentation

v0.1 SHOULD 提供基础 REST API 文档。

如果 Spring Boot 生态中能够低成本集成 OpenAPI / Swagger UI：

> 可以加入。

目的：

- 开发调试；
- Java API 展示；
- 前后端协作；
- 面试演示。

不得围绕 API Documentation 建设复杂平台。

---

# 30. Logging

Spring Boot 和 Python AI Service 都必须保留基础日志。

至少能够定位：

```text
Story ID

Stage ID

Chapter ID

Generation Request

AI Service Call

Memory Extraction

Failure
```

不得将：

- API Key；
- 完整敏感环境变量；

写入日志。

完整 LLM Prompt 是否记录，应根据开发调试需要决定，并注意小说内容体积。

---

# 31. Deployment

v0.1 的第一目标：

> **Local Development Success**

本地应能够运行：

```text
Vue

Spring Boot

Python AI Service

MySQL
```

公网部署属于：

> 核心闭环完成之后的工作。

---

# 32. Docker

Docker：

> **不是 v0.1 核心功能。**

如果核心开发完成后需要：

- 简化本地 MySQL；
- 简化服务启动；
- 最终部署；

可以增加：

```text
Dockerfile
docker-compose.yml
```

但不得因为 Docker 配置拖延核心功能。

---

# 33. Explicitly Excluded Technologies

以下技术 v0.1 默认禁止加入。

---

## 33.1 Redis

当前没有已验证的：

- 缓存瓶颈；
- 分布式锁需求；
- 高频共享状态问题。

因此：

> 不使用。

---

## 33.2 Kafka / RabbitMQ / RocketMQ

当前生成任务规模不需要 Message Queue。

因此：

> 不使用。

---

## 33.3 Spring Cloud

当前只有一个 Java Business Backend 和一个内部 Python AI Service。

不建设：

- Service Registry；
- Config Center；
- Gateway；
- Distributed Microservice Infrastructure。

---

## 33.4 Spring AI

当前 AI 学习方向明确使用：

> Python + LangChain。

不得同时加入 Spring AI 产生两套 AI 编排体系。

---

## 33.5 JPA / Hibernate

当前数据访问技术已经明确选择：

> MyBatis。

不得同时维护两套 Persistence Framework。

---

## 33.6 MyBatis-Plus

即使能够减少 CRUD 工作量，v0.1 暂时不使用。

主要原因：

> 项目同时承担真实 MyBatis + SQL 学习目标。

如果未来 CRUD 工作明显成为无价值重复劳动，可以重新评估。

---

## 33.7 Elasticsearch

当前没有全文搜索规模需求。

不使用。

---

## 33.8 Vector Database

v0.1 核心范围不要求 Vector Search。

因此不使用：

- Milvus；
- Pinecone；
- Weaviate；
- Qdrant；
- Elasticsearch Vector Search；

等独立 Vector Database。

---

## 33.9 Knowledge Graph / Graph Database

当前没有实验结果证明 Knowledge Graph 必要。

不使用：

- Neo4j；
- GraphRAG；
- 自研 Knowledge Graph Engine。

---

## 33.10 LangGraph

除非真实 Workflow 复杂度证明需要。

v0.1 不使用。

---

## 33.11 Kubernetes

v0.1 不涉及高并发和集群部署。

不使用。

---

# 34. Future Technology Candidates

以下技术不是“未来一定会使用”。

只是：

> **如果真实问题出现，可以重新评估。**

---

## Redis

Trigger：

> 高频 Current State 查询真的产生数据库性能问题。

---

## Message Queue

Trigger：

> 长时间 Generation Job 的可靠异步执行无法通过简单 Job 模型解决。

---

## Vector Retrieval

Trigger：

> Structured Memory 无法回答大量依赖历史原文的模糊查询。

---

## LangGraph

Trigger：

> AI Workflow 出现复杂循环、分支、checkpoint 和 human interrupt，普通代码明显难以维护。

---

## Temporal Storage

Trigger：

> Current Value 模型无法解决真实出现的历史状态查询问题。

---

# 35. Version Strategy

v0.1 不追求每个库永远使用“最新版本”。

选择原则：

1. Stable；
2. Compatible；
3. Non-preview；
4. 有正式文档；
5. 与项目核心技术兼容。

推荐版本系列：

```text
Java
21 LTS

Spring Boot
3.5.x

MyBatis
3.5.x

MyBatis Spring Boot Starter
3.x

MySQL
8.4 LTS

Vue
3.5.x

Python
3.12+

FastAPI
稳定版

LangChain Python
当前稳定主版本
```

具体 patch version 必须记录在：

```text
pom.xml
package-lock.json
Python dependency lock / requirements
```

而不是依赖本文档长期保持最新。

---

# 36. Dependency Change Rule

Coding Agent 不得擅自：

- 升级 Major Version；
- 更换 Framework；
- 引入新的 Database；
- 引入新的 Message Queue；
- 引入新的 AI Framework；
- 将 MyBatis 替换为 ORM；
- 将 Vue 替换为其他 Frontend Framework；
- 将 Java Business Backend 替换成 Python Backend。

如果发现当前技术无法完成需求：

必须：

```text
记录问题
↓
说明当前技术为什么无法解决
↓
提出最小替代方案
↓
等待项目决策
```

---

# 37. Resume-Oriented Technology Rule

由于项目使用 Vibecoding 开发，技术栈必须保持：

> 易启动、易验证、易恢复。

开发环境最终应尽量允许：

```text
Backend Start

AI Service Start

Frontend Start

Database Start
```

通过简单明确的命令完成。

禁止建立依赖大量人工操作才能恢复的开发环境。

---

# 38. Resume Relevance

如果 v0.1 成功完成，项目最终可以真实体现：

### Java Backend

```text
Java 21
Spring Boot
REST API
Validation
MyBatis
MySQL
Transaction
HTTP Service Integration
Exception Handling
Business State Management
```

### AI Engineering

```text
Python
FastAPI
LangChain
LLM Integration
Structured Output
Prompt / Context Construction
Memory Extraction
AI Workflow
```

### Frontend

```text
Vue 3
TypeScript
Vite
Vue Router
Pinia
REST API Integration
```

但简历最终只允许写：

> **项目中真正实现、真正使用并且开发者能够解释的技术。**

---

# 39. Technology Learning Rule

对于项目中的每一个核心技术，开发完成后开发者至少应该能够回答：

1. 为什么这个项目需要它？
2. 它在系统中负责什么？
3. 为什么没有使用更简单或更常见的替代方案？
4. 它与其他模块如何通信？
5. 如果它发生故障，会影响什么？
6. 项目中具体哪段业务使用了它？

如果无法回答：

> 该技术不得仅因为“AI写进去了”而成为简历技术亮点。

---

# 40. Final v0.1 Stack

v0.1 默认技术栈锁定为：

```text
Frontend
────────────────────────
Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios


Main Backend
────────────────────────
Java 21
Spring Boot 3.5.x
Spring Web
Spring Validation
MyBatis
MyBatis Spring Boot Starter
MySQL Connector/J
Maven


Database
────────────────────────
MySQL 8.4 LTS


AI Service
────────────────────────
Python 3.12+
FastAPI
Pydantic
LangChain
LLM Provider SDK


Testing
────────────────────────
JUnit 5
Spring Boot Test
pytest


Infrastructure
────────────────────────
Git
Environment Variables

Optional after core completion:
Docker / Docker Compose
```

任何不在此列表中的核心技术：

> 默认不属于 v0.1。