# PRODUCT_SPEC.md

# AI Story Co-Author v0.1.1

## Product Position

作者是导演和最终编辑，AI 是联合写作者。AI 提出和执行，作者拥有方向、计划、正文和最终文本控制权。

## Main Workflow

```text
Create Story
→ Set target chapter count / style / default length
→ Create or select Arc
→ Enter Stage Direction
→ Planner continues from current story
→ Author reviews ChapterSpecs
→ Confirm
→ Generate Draft
→ Extract Memory
→ Edit / Regenerate / Polish / Approve
→ Re-extract if revision changed
→ Continue / Replan Remaining / New Stage
```

## Story Settings

`name / coreIdea / constraints / defaultTargetCharacters / targetChapterCount / writingStyle`。默认建议 `defaultTargetCharacters=3000`。

`writingStyle` 使用自然语言 Profile，不做大量 Slider；不承诺 AI 检测规避。

## Arc

Arc 是数十章级宏观阶段：`title / goal / targetStartChapter / targetEndChapter / status`。采用 Rolling Planning，不一次详细规划 600 章。

## Stage Planning

Planner 必须获得 Core Idea、Constraints、Long-form Position、Current Arc、Completed Stage Summaries、Current State、Relationship、Selected Memory、Recent Summaries、Continuation Anchor、New Direction。

Continuation Anchor 至少：`lastChapterNumber / currentLocation / activeCharacters / currentImmediateGoal / lastChapterSummary / lastChapterEnding`。

## ChapterSpec

至少：

```text
goal
expectedProgress
targetCharacters
mustAdvance[]
mustNotDo[]
storyBeats[]
endingIntent
```

Writer 以 ChapterSpec 为本章主目标，Memory 只提供事实支持。

## Recent Context

Writer 使用前 2–3 章 summaries + last chapter ending excerpt，不再只依赖单个上一章 Summary。Summary 重点记录本章完成事件、状态变化、未完成动作和结尾位置。

## Length

优先级：`Chapter override > Stage override > Story default`。记录 `targetCharacters / actualCharacterCount`。

## Chapter Revision

AI 首稿为 `DRAFT`。支持 Manual Edit、Regenerate、AI Polish、Approve。Revision Source：`AI_GENERATED / MANUAL_EDIT / AI_POLISH / AI_REWRITE`。

Polish 可以改句式、对白、重复和场景表现；默认禁止改变核心事件、重大设定、人物最终状态和 Ending Intent。

当前 Revision 改变后：`memoryExtractionStatus=STALE → re-extract → reconcile → COMPLETED`。

## Replan Remaining

例如 V1 1–9 已完成 1–3，作者要求剩余 3 章：保留 1–3，旧 4–9 superseded，新 V2 4–6 从当前真实 Story State 继续。

## Generation Modes

STEP：单章 checkpoint 后暂停。CONTINUOUS：后台执行，前端 polling。Pause 在安全 checkpoint 生效，Stop 保留已完成 Chapter 并停止剩余计划。

Recovery 根据数据库事实：Chapter missing → Generate；Chapter exists + Memory PENDING/FAILED/STALE → Extract；checkpoint complete → Next Plan。

## Memory v2

Types：`CURRENT_STATE / RELATIONSHIP / PLOT_FACT / PLOT_THREAD / FORESHADOWING / WORLD_RULE / TRANSIENT_DETAIL`。

Importance：1–5；Scope：CHAPTER/STAGE/ARC/STORY。普通面包可以是 `TRANSIENT_DETAIL, importance=1, scope=CHAPTER`。

Writer 不接收全部 Memory。Current State、相关 Relationship、importance=5 常驻；当前 Arc/Stage/ChapterSpec 相关 memory 可选；TRANSIENT_DETAIL 与 importance<=2 默认排除。

Inventory 多物品使用独立 field，例如 `item:iron_sword=owned`、`item:potion=owned`。

## Non-goals

不实现 RAG/Vector、KG、Temporal Engine、Volume、自动发布、AI 检测规避、自动多 Agent Runtime、600 章全量生成。
