# MVP_SCOPE.md

# AI Story Co-Author v0.1.1 — Frozen Scope

## Mission

> 让作者能够在长期故事目标约束下，从已有剧情持续续写，控制每章长度和章节目标，修改 AI 草稿，并让 Memory 真正辅助而不是劫持剧情。

## MUST

1. **Continuation**：Planner 使用真实 Current State / Relationship / Selected Memory / Recent Context / Continuation Anchor；新 Stage 不重新开书。
2. **Writer Context**：Writer 真正获得 State / Relationship / Selected Memory，不再正式传空列表。
3. **ChapterSpec**：`goal / expectedProgress / targetCharacters / mustAdvance / mustNotDo / storyBeats / endingIntent` 全链路保存并被 Writer 使用。
4. **Length Control**：Story 默认章节字数，Stage/Chapter 可覆盖。
5. **Replan Remaining**：保留已完成 Chapter 和历史 Plan，只重规划未来。
6. **Chapter Revision**：Manual Edit / Regenerate / AI Polish / Revision History / Approve。
7. **Revision → Memory Refresh**：当前正文改变后旧 extraction STALE，重新提取。
8. **Long-form Pace**：`targetChapterCount` + `Story → Arc → Stage → Chapter`，600 章前期不默认推进终局。
9. **Memory v2**：固定 type、importance 1–5、scope、TRANSIENT_DETAIL、dedup v1、多物品 Inventory、Selected Memory。
10. **Generation Reliability**：Extraction Retry、Async Continuous、Polling、Pause、Stop、Stage COMPLETED、数据库事实驱动 Recovery。
11. **Real-LLM Acceptance**：语义行为必须真实 LLM 验收。

## Minimal Domain Model

```text
Story
├── Constraints
├── Long-form Settings
├── Arc
│   └── Stage
│       ├── Plan Version → ChapterSpec
│       └── Chapter → ChapterRevision
├── Current State
├── Relationship State
├── Story Memory
└── Memory Candidate
```

不增加 Volume。

## Frozen Out of Scope

RAG / Embedding / Vector DB / GraphRAG / Knowledge Graph / Temporal Truth / Canon Governance / LangGraph / Spring AI / Redis / MQ / Spring Cloud / Kubernetes / Volume / Auth / Multi-user / Payment / Auto Publishing / AI Detection Bypass / Fine-tuning / 600 章全量生成 / 商业富文本编辑器。

## Definition of Done

Engineering：Backend tests、Python tests、Frontend build、additive migration、Extraction Retry、Async/Pause/Replan 状态一致。

Real LLM：Continuation、Length、Chapter Goal、Replan Remaining、Manual Edit workflow、Polish Fact Preservation、600 Chapter Pace、Low-value Detail Isolation。

只有证据齐全后才能声明 `v0.1.1 ACCEPTED`。
