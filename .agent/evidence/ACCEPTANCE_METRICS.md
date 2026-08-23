# v0.1.1 Acceptance Metrics

## Current Release Gate

```text
Required final model: qwen3.7-plus
Engineering suite:    PASSED RH-09 (backend 70 · Python 14 · frontend 6/build)
Real-LLM suite:       NOT_RUN — PENDING RH-10
Release verdict:      NOT ACCEPTED
```

Final metrics will be written from one complete RH-10 run after RH-09 passes.
No earlier partial model run is promoted into the release verdict.

## Historical qwen3-8b Record

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

The 2026-08-22 qwen3-8b run is retained only for diagnosis. It recorded an
AC-103 length pass rate of 0/5 at the frozen 2250-character lower bound, while
the other exercised semantic checks passed. This evidence neither passes nor
fails the required qwen3.7-plus RH-10 suite.

Detailed raw artifacts remain under `.agent/evidence/`.
