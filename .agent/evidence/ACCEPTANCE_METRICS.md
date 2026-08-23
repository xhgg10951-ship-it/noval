# v0.1.1 Acceptance Metrics

## Current Release Gate

```text
Required final model: qwen3.7-plus
Engineering suite:    NOT RUN for current candidate — RH-09 pending
Current RH-10 status:      NOT RUN after reopened functional repairs
Hosted CI:            NOT RUN for current candidate
Current release verdict:  NOT ACCEPTED
```

The metrics below belong to the pre-reopen run at product commit `c5e53cc`.
They are retained as historical comparison evidence and do not accept the
current candidate.

Run ID: `rh10_qwen3.7-plus_c5e53cc_20260823`

Product/prompt commit: `c5e53cc`

RH-10 evidence commit: `95908cc`

Hosted CI: [run 32643065616](https://github.com/xhgg10951-ship-it/noval/actions/runs/32643065616)

Raw evidence: `.agent/evidence/rh10_qwen3.7-plus_c5e53cc_20260823/`

| Metric | Final value |
|---|---|
| Model | qwen3.7-plus (`mock_llm=false`) |
| Story ID | N/A — direct-contract semantic fixture; no persisted Story row |
| Target / actual characters | 3000 / 2896, 2828, 3155, 2874, 2843 |
| Length pass rate | 5/5 in 2250–3750 |
| Obvious padding | PASS; 0 exact duplicate long sentences |
| Chapter-goal completion | 3/3 advance groups; 0 hidden-identity violations |
| Continuation failures | 0 |
| Low-value detail repetition | 0/3 later chapters |
| Duplicate Memory | 0 in fixture; deterministic dedup gate passed at RH-09 |
| Manual Edit / Regenerate | all three revision regressions passed at RH-01 |
| Polish count | 3 real-LLM requests (one evidence-format rerun, one checker false negative); final 7/7 checks |
| Memory extraction failures | 0 |
| Average AC-103 chapter generation latency | 34.63 seconds/chapter |

## Historical Records

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

The pre-reopen qwen3.7-plus run above and the 2026-08-22 qwen3-8b run are
retained only for diagnosis. The qwen3-8b run recorded an
AC-103 length pass rate of 0/5 at the frozen 2250-character lower bound, while
the other exercised semantic checks passed. This evidence neither passes nor
fails the required qwen3.7-plus RH-10 suite.

Detailed historical raw artifacts remain under `.agent/evidence/` outside the
RH-10 run directory.
