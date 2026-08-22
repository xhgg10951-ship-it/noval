# AGENTS.md

## 1. Current Version

```text
Active Version: v0.1.1
Requirement Status: FROZEN
Development Branch: v0.1.1-dev
Baseline: v0.1.0
```

v0.1.1 是在现有 v0.1 工程基线上进行的增量迭代，不是重写项目。

> **Human defines direction and boundaries. Agent executes autonomously inside those boundaries.**

## 2. Document Authority

发生冲突时：

1. `AGENTS.md` — Agent 行为与安全规则
2. `V0.1.1_IMPROVEMENT_PLAN.md` — v0.1.1 冻结需求、根因、变更控制
3. `MVP_SCOPE.md` — 当前范围边界
4. `PRODUCT_SPEC.md` — 当前产品行为
5. `ACCEPTANCE_TESTS.md` — 当前验收标准
6. `ARCHITECTURE.md` — 当前架构边界
7. `TECH_STACK.md` — 技术栈
8. `PROJECT_VISION.md` — 长期愿景

Git tag `v0.1.0`、`docs/history/`、`.agent/history/` 只用于历史回溯，不是当前规范。

## 3. Human-Owned Documents

Agent 不得擅自改变以下文档的需求含义：

```text
PROJECT_VISION.md
V0.1.1_IMPROVEMENT_PLAN.md
MVP_SCOPE.md
PRODUCT_SPEC.md
ACCEPTANCE_TESTS.md
TECH_STACK.md
ARCHITECTURE.md
AGENTS.md
```

若冻结设计无法通过现有验收，必须将任务标记 `BLOCKED`，并记录：Observed blocker / Why frozen design fails / Smallest required change / Alternatives attempted / New complexity。

## 4. Agent-Owned Documents

必须维护：

```text
.agent/TASKS.md
.agent/STATE.md
```

真实实验可记录在 `.agent/EXPERIMENT.md`。

## 5. Recovery Order

新会话按顺序：

```text
AGENTS.md
V0.1.1_IMPROVEMENT_PLAN.md
.agent/STATE.md
.agent/TASKS.md
MVP_SCOPE.md
PRODUCT_SPEC.md
ACCEPTANCE_TESTS.md
ARCHITECTURE.md
TECH_STACK.md
git branch --show-current
git status
git diff
git log --oneline -5
```

预期开发分支为 `v0.1.1-dev`。如果在 `main`，不要修改源码。禁止 force push、自动 reset、删除未知修改或 orphan rewrite。

## 6. Execution Rules

- One active implementation task at a time.
- 按 `.agent/TASKS.md` Phase 0 → Phase 9 执行。
- Phase Gate 未通过，不进入下一 Phase。
- 一个验证完成的原子任务对应一个清晰 commit。
- v0.1.1 只做增量修改，不重建 Spring/Vue/Python 工程。

## 7. DONE Integrity

> **Code exists ≠ Feature works.**

> **DTO exists ≠ Context is wired.**

> **Mock passes ≠ AI behavior passes.**

> **STATE says DONE ≠ Repository proves DONE.**

所有 AI 行为任务记录：

```text
Engineering Verification: PASSED / FAILED / NOT_RUN
Real-LLM Semantic Verification: PASSED / FAILED / NOT_REQUIRED / BLOCKED
```

## 8. Mock Boundary

Mock 可以证明 HTTP、DTO、解析、持久化、Context Wiring、Retry、Workflow、状态机。

Mock 不可以证明文风、Planner 续写质量、ChapterSpec 语义遵循、Memory 改善正文、600 章节奏、Polish 质量。

> **Mock proves plumbing. Real LLM proves AI behavior.**

## 9. Architecture Boundary

> **AI proposes. Java decides. MySQL remembers.**

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

Vue 只负责 UI；不得直接调用 Python。Spring Boot 负责业务状态、Context Assembly、Story/Arc/Stage、ChapterSpec、ChapterRevision、GenerationJob、Memory 应用、Retry 和 MySQL。Python 只负责 Planner、Writer、Memory Extractor、Polish、Query/Suggestions。

## 10. Context Priority

Writer Context：

```text
Hard Constraints
> Long-form Position
> Current Arc
> Current Stage
> Current ChapterSpec
> Current State
> Selected Memory
> Recent Narrative Context
```

Memory 只支持事实，不替换本章主线。

## 11. Replan / Revision / Recovery

作者可以 `Replan Remaining`，但不得删除已完成 Chapter 对应历史 Plan。AI 不能擅自扩展确认计划。

AI 生成 Chapter 默认为 `DRAFT`，作者可以 Manual Edit / Regenerate / AI Polish / Approve。Revision 改变后 Memory 必须 STALE → re-extract。

若 Chapter 已保存但 Memory Extraction FAILED，Retry 必须先补同章 Memory，不能跳下一 Plan。Continuous 使用 Spring 应用内后台 executor，不引入 MQ。

## 12. Frozen Out-of-Scope

不得加入：RAG、Embedding、Vector DB、GraphRAG、Knowledge Graph、Temporal Truth、Canon Governance、LangGraph、Spring AI、Redis、Kafka/RabbitMQ/RocketMQ、Spring Cloud、Kubernetes、Volume、Auth/Multi-user、Auto Publishing、AI Detection Bypass、Fine-tuning、Automatic Multi-Agent Story Room。

## 13. Database Rule

v0.1 数据必须保留。优先 `CREATE TABLE / ADD COLUMN / safe default / backfill`，不得为方便开发清空数据库。

## 14. Current Start

```text
Version: v0.1.1
Phase: Phase 0
Task: TASK-101
```

首先保存 v0.1 regression evidence，不要跳过 Phase 0。

> **Fix observed behavior before adding architecture.**
