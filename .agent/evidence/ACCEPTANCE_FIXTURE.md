# v0.1.1 Acceptance Fixture & Run Record (TASK-172)

> Frozen before execution. Do not tune the fixture to make results pass.

## Model / Prompt Versions

```text
Model:            qwen3-8b (Aliyun MaaS compatible-mode, mock_llm=False)
Prompt versions:  builders.py @ commit 87c66cb+ (v0.1.1 Phase 8)
                  - planner: continuation framing + completed-beats + pace guard
                  - writer:  goal-lock priority + style block (TASK-166)
                  - extractor: five-question discipline + type whitelist (TASK-160)
                  - polish:   fact-preservation rules (TASK-168)
Backend:          v0.1.1-dev @ d2df268..HEAD
DB:               local MySQL story_ai, migrations V1..V14 applied
```

## Fixed Story Settings

```text
coreIdea:                现代青年林夜穿越到剑与魔法世界，结识冒险者艾琳并被她收留。
targetChapterCount:      600
defaultTargetCharacters: 3000
writingStyle:            冷峻克制，多用具体感官细节，少用形容词堆砌
Arc #1:                  第一卷·初入异界, ch 1–60,
                         goal=在城镇立足、成为正式冒险者并完成初期委托, ACTIVE
Stage direction (AC-114): 第二天前往冒险者公会入会并接取委托：调查幽影森林失踪案
```

## Chapter Specs (planned by real LLM under this fixture)

```text
ch1 入会/前往公会 → ch2 委托背景与新势力 → ch3 关系深化/成长
→ ch4 外部威胁引入 → ch5 完成当前卷目标（为后续铺垫）
```

## Acceptance Runs

| AC | Scope | Evidence |
|----|-------|----------|
| AC-101 | Planner continues from established state | `.agent/evidence/` AC101_* files (Phase 1 run) |
| AC-103 | Writer length control | honest FAIL — model ceiling ~1800–2000 chars (recorded Phase 2) |
| AC-104 | Goal adherence (mustAdvance/mustNotDo) | EVIDENCE_AC104_*.json (Phase 2 run) |
| AC-105 | Bread-loop isolation | `ac105_extract.json` + `.agent/ac105_run.py` (Phase 7 run) |
| AC-106 | Replan Remaining preserves history | `ac106v2_after_replan.json` + DB assertions (Phase 4 run) |
| AC-109 | Polish fact preservation | `ac109_polish.json` + structured checks 7/7 (Phase 8 run) |
| AC-114 | 600-chapter pace guard | `ac114_plan.json` + `.agent/ac114_run.py` (Phase 6 run) |

## Engineering Suite

```text
backend:  mvn test  -> 59/59 PASSED
python:   pytest    -> 7/7  PASSED
frontend: npm build -> SUCCESS
migrations: V1..V14 applied to story_ai (verified via information_schema checks
            during Phases 3/5/6/7)
```
