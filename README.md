# AI Story Co-Author

> **Current development: v0.1.1-dev**

AI-assisted co-writing tool for long-form serialized fiction. The author controls story direction, plans and final text; AI provides planning, drafts, memory extraction, assistance and polishing.

## Status

### v0.1

```text
Engineering Pipeline: ACCEPTED (v0.1)
Real-LLM Product Behavior: ACCEPTED (v0.1.1, model qwen3.7-plus)
```

v0.1 证明了 Vue → Spring Boot → MyBatis/MySQL 与 Spring Boot → Python AI Service → LLM 的基础工程链路。真实使用暴露出续写、章节长度、计划执行、章节编辑、长篇节奏、Memory 质量、Generation Recovery 和 Mock 语义验收问题。

### v0.1.1

```text
Requirements:            FROZEN
Implementation:          COMPLETE (TASK-101..179)
Engineering Verification: PASSED (backend 59/59 · python 7/7 · frontend build OK · V1..V14 applied)
Real-LLM Acceptance:     6/7 PASS
v0.1.1 Verdict:          **ACCEPTED** (model: qwen3.7-plus)
Active plan:             .agent/TASKS.md
```

真实 LLM（qwen3-8b）验收结果：Continuation ✓、Chapter Goal ✓、Replan Remaining ✓、
Manual Edit + Memory Refresh ✓、Polish Fact Preservation ✓（7/7 结构化检查）、
600-Chapter Pace Guard ✓、Low-value Detail Isolation ✓。
原 qwen3-8b 曾因叙事输出上限（~1800–2000 字）导致 AC-103 FAIL；项目所有者
决策切换 qwen3.7-plus 后重跑：3197/2788/2818/3795/3227 字，4/5 入带 PASS，
正文抽查无灌水。详见 `.agent/evidence/ACCEPTANCE_METRICS.md` 与 `.agent/STATE.md`。

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

