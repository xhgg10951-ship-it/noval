# AI Story Co-Author

> **Current development: v0.1.1-dev**

AI-assisted co-writing tool for long-form serialized fiction. The author controls story direction, plans and final text; AI provides planning, drafts, memory extraction, assistance and polishing.

## Status

### v0.1

```text
Engineering Pipeline: ACCEPTED
Real-LLM Product Behavior: NOT ACCEPTED
```

v0.1 证明了 Vue → Spring Boot → MyBatis/MySQL 与 Spring Boot → Python AI Service → LLM 的基础工程链路。真实使用暴露出续写、章节长度、计划执行、章节编辑、长篇节奏、Memory 质量、Generation Recovery 和 Mock 语义验收问题。

### v0.1.1

```text
Requirements: FROZEN
Implementation: IN PROGRESS
Active plan: .agent/TASKS.md
```

目标：Continuation-aware Planning、Writer Context 真接通、ChapterSpec + Length、Replan Remaining、Chapter Revision、Arc Pace、Memory v2、可靠 Generation Recovery、Real-LLM Acceptance。

## Architecture

> **AI proposes. Java decides. MySQL remembers.**

> **The plan guides. Memory supports.**

> **Generated text is a draft until the author accepts it.**

```text
Vue 3 → Spring Boot → MyBatis/MySQL
                    → Python FastAPI/LangChain → LLM
```

## Active Documents

1. `AGENTS.md`
2. `V0.1.1_IMPROVEMENT_PLAN.md`
3. `.agent/STATE.md`
4. `.agent/TASKS.md`
5. `MVP_SCOPE.md`
6. `PRODUCT_SPEC.md`
7. `ACCEPTANCE_TESTS.md`
8. `ARCHITECTURE.md`
9. `TECH_STACK.md`

`PROJECT_VISION.md` 仍是长期愿景。

## Validation Rule

> **Mock proves plumbing. Real LLM proves AI behavior.**

当前启动方式仍参考 `RUN.md`；只有实现改变运行方式时再同步 RUN。
