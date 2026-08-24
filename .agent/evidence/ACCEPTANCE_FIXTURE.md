# v0.1.1 Final Acceptance Fixture

> Frozen inputs must not be tuned after seeing results.

## Release Configuration

```text
Required model:     qwen3.7-plus (mock_llm=false)
Prompt/code commit: 4696b4f
RH-10 evidence:     3aafee0
Branch:             v0.1.1-dev
Database:           MySQL story_ai
Migrations:         V1..V16
Engineering suite:  PASSED — backend 86/86, Python 23/23, frontend 8/8 + build
Run ID:             rh10_qwen3.7-plus_4696b4f_focus_20260824_product
Hosted CI:          PASSED — run 32676289823 at 3aafee0
Release verdict:    ACCEPTED
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
- AC-109 Polish fact preservation
- AC-114 600-chapter pace guard
- release-hardening semantic cases required by `V0.1.1_RELEASE_HARDENING.md`

The next RH-10 must execute every result from one fixed qwen3.7-plus release
configuration and record it in `ACCEPTANCE_METRICS.md` before RH-11.

## Fixed Product-wired Execution Contract

The current run uses `scripts/rh10_product_suite.py` and talks only to the
public Spring Boot API. It does not call an AI endpoint, prompt builder, or LLM
provider directly.

- Story A creates the frozen 600-chapter Story and Arc 1–60. A two-chapter
  Stage persists meeting Erin and being housed. A three-chapter second Stage
  then exercises AC-101 and produces five consecutive 3000-character chapters
  for AC-103. Its first chapter must complete guild registration without an
  identity disclosure for AC-104.
- Before the second Stage, Manual Edit adds one incidental ordinary-bread
  sentence. The product's automatic re-extraction must classify it as
  `TRANSIENT_DETAIL / 1 / CHAPTER / IGNORE`; the prior extraction's candidates
  must all become `SUPERSEDED`. The next three unrelated ChapterSpecs must not
  reproduce ordinary bread or make bread a sustained narrative focus. The
  fixed evaluator rejects any `普通面包` recurrence, a per-chapter bread-sentence
  ratio above 3%, or focus in all three chapters; incidental independent world
  details are not treated as narrative hijacking.
- AC-109 polishes a fixed deliberately mechanical chapter through the public
  revision API. Every named character, location, item, case clue and ending
  intent must remain, while repeated sentence starts decrease.
- At exactly current chapter 5, a new five-plan Stage is generated under
  target=600 and Arc 1–60. Orders must be 6–10, at least one local-progress
  term must occur, and no frozen endgame pattern may occur for AC-114.
- Story B creates a nine-plan Stage, generates three chapters, stops the STEP
  Job, replans the remainder to three, and generates the new remainder. AC-106
  requires logical orders 4–6, unchanged first-three IDs/content/revisions,
  final chapter numbers 1–6, and no duplicate normalized titles.

AC-103 requires at least 4/5 persisted `actualCharacterCount` values in
2250–3750 and zero exact duplicate sentences of 24 or more non-space
characters across the five chapters. These inputs and the focus-based evaluator
were frozen before the final `4696b4f_focus` qwen3.7-plus output was generated.

## Historical Fixture Notice

> **HISTORICAL / SUPERSEDED FOR FINAL RELEASE VERDICT**

The earlier qwen3-8b fixture and pre-reopen qwen3.7-plus run remain in Git
history and raw evidence files for diagnosis only; neither is the final release
fixture for the current repaired candidate.
