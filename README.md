# AI Story Co-Author

> Active development: `v0.1.1-dev`

AI-assisted co-writing for long-form serialized fiction. The author controls
direction, plans, revisions, and final text; AI proposes plans, drafts, memory,
assistance, and polish.

## Current Release Status

```text
Requirements:             FROZEN
Implementation:           Feature Complete
Release Hardening:        RH-01..RH-11 complete
Engineering Verification: PASSED — backend 86/86, Python 23/23, frontend 8/8 + build
Required Final Model:     qwen3.7-plus
Current Real-LLM Gate:    PASSED — 7/7 product-wired suite at product commit 4696b4f
Hosted CI:                PASSED — run 32676289823 at RH-10 evidence commit 3aafee0
Current Release Verdict:  ACCEPTED
```

The earlier RH-11 verdict was withdrawn after the functional re-audit at
`36ac510`. The repaired `v0.1.1-dev` candidate subsequently passed the strict
RH-01 through RH-11 sequence, including the complete product-wired
qwen3.7-plus gate and current hosted CI. This is the verified v0.1.1 release
candidate.

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
