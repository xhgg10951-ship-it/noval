# v0.1.1 Acceptance Metrics

## Current Release Gate

```text
Required final model: qwen3.7-plus
Engineering suite:    PASSED — backend 86/86, Python 23/23, frontend 8/8 + build
Current RH-10 status: PASSED — 7/7
Hosted CI:            PASSED — run 32676289823 at 3aafee0
Current release verdict:  ACCEPTED
```

Current run ID: `rh10_qwen3.7-plus_4696b4f_focus_20260824_product`

Product/prompt commit: `4696b4f`

RH-10 evidence commit: `3aafee0`

Hosted CI: [run 32676289823](https://github.com/xhgg10951-ship-it/noval/actions/runs/32676289823)

Raw evidence: `.agent/evidence/rh10_qwen3.7-plus_4696b4f_focus_20260824_product/`

| Current metric | Final value |
|---|---|
| Model | qwen3.7-plus (`mock_llm=false`) |
| Prompt version | `ai-service/app/prompts/builders.py@4696b4f` |
| Story IDs | 1830 (continuation/length/goal/low-value/polish/pace), 1831 (replan) |
| Product boundary | public Spring API → Java workflow/context → Python AI → MySQL |
| Frozen semantic suite | AC-101/103/104/105/106/109/114: 7/7 PASSED |
| Target / actual characters | 3000 / 3604, 3036, 3556, 3040, 3322 |
| Length pass rate | 5/5 in 2250–3750; zero duplicate long sentences |
| Chapter goal completion | 1/1; guild registration completed; zero identity-disclosure violations |
| Continuation failures | 0 |
| Low-value detail | exact TRANSIENT_DETAIL/1/CHAPTER/IGNORE; 10/10 old candidates SUPERSEDED; ordinary-bread recurrence 0/3; focus ratios 1.94%/0%/0% |
| Duplicate active Memory | 0 exact normalized duplicates among 10 active StoryMemory rows in the two final snapshots |
| Revision operations | Manual Edit 2; Regenerate 0; Polish 1 |
| Replan Remaining | old job STOPPED; final job COMPLETED; chapters 1–3 unchanged; final numbers 1–6 |
| Polish | all characters/location/items/case/ending facts retained; mechanical starts 6→1 |
| 600-chapter pace | logical orders 6–10; no endgame pattern; local forest/loss/survival progress |
| Memory extraction failures | 0 |
| Average generation latency | 61.9 seconds/chapter across 11 generated chapters, derived from persisted Job created/updated checkpoints; includes extraction workflow |

## Historical Records

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

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

The pre-reopen qwen3.7-plus run above and the 2026-08-22 qwen3-8b run are
retained only for diagnosis. The qwen3-8b run recorded an
AC-103 length pass rate of 0/5 at the frozen 2250-character lower bound, while
the other exercised semantic checks passed. This evidence neither passes nor
fails the required qwen3.7-plus RH-10 suite.

Detailed historical raw artifacts remain under `.agent/evidence/` outside the
RH-10 run directory.
