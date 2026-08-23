# v0.1.1 Final Acceptance Fixture

> Frozen inputs must not be tuned after seeing results.

## Release Configuration

```text
Required model:     qwen3.7-plus (mock_llm=false)
Prompt/code commit: PENDING RH-10
Branch:             v0.1.1-dev
Database:           MySQL story_ai
Migrations:         V1..V16
Engineering suite:  PENDING RH-09
Run ID:             PENDING RH-10
Hosted CI:          PENDING current candidate
Release verdict:    NOT ACCEPTED
```

## Fixed Story Settings

```text
coreIdea:                现代青年林夜穿越到剑与魔法世界，结识冒险者艾琳并被她收留。
targetChapterCount:      600
defaultTargetCharacters: 3000
writingStyle:            冷峻克制，多用具体感官细节，少用形容词堆砌
Arc #1:                  第一卷·初入异界, ch 1–60,
                         goal=在城镇立足、成为正式冒险者并完成初期委托, ACTIVE
Stage direction:         第二天前往冒险者公会入会并接取委托：调查幽影森林失踪案
```

## Required Full RH-10 Suite

- AC-101 continuation
- AC-103 dynamic chapter length
- AC-104 ChapterSpec goal adherence
- AC-105 low-value-detail isolation
- AC-106 Replan Remaining
- AC-107 Manual Edit plus Memory refresh
- AC-109 Polish fact preservation
- AC-114 600-chapter pace guard
- release-hardening semantic cases required by `V0.1.1_RELEASE_HARDENING.md`

The next RH-10 must execute every result from one fixed qwen3.7-plus release
configuration and record it in `ACCEPTANCE_METRICS.md` before RH-11.

## Historical Fixture Notice

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

The earlier qwen3-8b fixture and pre-reopen qwen3.7-plus run remain in Git
history and raw evidence files for diagnosis only; neither is the final release
fixture for the current repaired candidate.
