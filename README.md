# AI Story Co-Author

> Active development: `v0.1.1-dev`

AI-assisted co-writing for long-form serialized fiction. The author controls
direction, plans, revisions, and final text; AI proposes plans, drafts, memory,
assistance, and polish.

## Current Release Status

```text
Requirements:             FROZEN
Implementation:           Feature Complete, release repairs in progress
Release Hardening:        RH-01..RH-07 complete; RH-08 active
Engineering Verification: PARTIAL — per-RH suites green; RH-09 pending
Required Final Model:     qwen3.7-plus
Current Real-LLM Gate:    NOT RUN after reopened repairs; RH-10 pending
Hosted CI:                NOT RUN for the current candidate; RH-09 pending
Current Release Verdict:  NOT ACCEPTED
```

The earlier RH-11 verdict was withdrawn after the functional re-audit at
`36ac510`. The current `v0.1.1-dev` candidate must complete RH-08/RH-09, then
pass a fresh product-wired qwen3.7-plus RH-10 run before RH-11 can accept it.

Earlier qwen3-8b results and the pre-reopen qwen3.7-plus run are retained as
historical evidence only:

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
