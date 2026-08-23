# AI Story Co-Author

> Active development: `v0.1.1-dev`

AI-assisted co-writing for long-form serialized fiction. The author controls
direction, plans, revisions, and final text; AI proposes plans, drafts, memory,
assistance, and polish.

## Current Release Status

```text
Requirements:             FROZEN
Implementation:           Feature Complete
Release Hardening:        RH-01..RH-08 complete; RH-09 next
Required Final Model:     qwen3.7-plus
Final Real-LLM Suite:     NOT_RUN (RH-10)
v0.1.1 Verdict:           NOT ACCEPTED
```

v0.1.1 may be marked ACCEPTED only after RH-01 through RH-10 pass and the full
RH-10 real-LLM suite passes on `qwen3.7-plus`. The old TASK-179 verdict is
superseded by `V0.1.1_RELEASE_HARDENING.md`.

Historical qwen3-8b results are retained as evidence only:

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

They do not prove the current release and must not be used as the final model
verdict.

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
2. `V0.1.1_RELEASE_HARDENING.md`
3. `V0.1.1_IMPROVEMENT_PLAN.md`
4. `.agent/STATE.md`
5. `.agent/TASKS.md`
6. `MVP_SCOPE.md`
7. `PRODUCT_SPEC.md`
8. `ACCEPTANCE_TESTS.md`
9. `ARCHITECTURE.md`
10. `TECH_STACK.md`

`PROJECT_VISION.md` is the long-term vision. Local setup and verification are
documented in `RUN.md`.

## Verification Rule

> **Mock proves plumbing. Real LLM proves AI behavior.**
