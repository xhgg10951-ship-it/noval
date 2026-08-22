# v0.1.1 Acceptance Metrics (TASK-175)

Model: qwen3-8b (Aliyun MaaS compatible-mode) · Run date: 2026-08-22

| Metric | Value | Source |
|---|---|---|
| Model | qwen3-8b | health endpoint mock_llm=false |
| Prompt Version | builders.py @ Phase 8 (87c66cb+) | git |
| Story ID (fixture runs) | 479 / 364 / 265 / 264 | .agent/evidence/ac*.txt |
| Target Characters (per chapter) | 3000 default | story settings |
| Actual Characters | 338–2000+ per chapter; qwen3-8b ceiling ~1800–2000 | AC-103 runs |
| Length Pass Rate (AC-103, ≥2250 chars) | **0/5 — FAIL** | Phase 2 honest record |
| Chapter Goal Completion Rate (AC-104) | mustAdvance 3/3 keywords hit; mustNotDo 0 violations → PASS | EVIDENCE_AC104_* |
| Continuation Failure Count (AC-101/106/114) | 0 (no re-crossing / no restart observed in final runs) | ac106v2/ac114 evidence |
| Low-value Detail Repetition Count (AC-105) | 0 bread candidates reach writer context | ac105_extract.json |
| Duplicate Memory Count after dedup (AC-115) | 1 row for duplicate fact (was 2 pre-dedup scenario) | MemoryV2IntegrationTest |
| Manual Edit Count (AC-107 fixture) | 1 edit → v2 MANUAL_EDIT → STALE → re-extract COMPLETED | Phase 5 DB records |
| Regeneration Count | covered by integration suite (AI_REWRITE path verified) | ChapterRevisionIntegrationTest |
| Polish Count (AC-109) | 1 polish → AI_POLISH revision, facts preserved 7/7 checks | ac109_polish.json |
| Memory Extraction Failure Count | 0 in final acceptance runs (failure paths covered by TASK-132 suite) | GenerationReliabilityRegressionTest |
| Average Generation Latency | planner ≈3–7 s · writer ≈8–60 s/chapter · extract ≈2–5 s | phase run timings |

## Known Limitations (honest)

1. **AC-103 length ceiling**: qwen3-8b cannot stably exceed ~1800–2000 chars
   per chapter; the frozen 2250 lower bound is unreachable with this model.
   Remediation requires a larger model or segmented generation (deferred).
2. Extractor may omit importance/scope fields on some outputs; Java
   clamp/normalize fallback applies (importance→3, scope→STORY).
