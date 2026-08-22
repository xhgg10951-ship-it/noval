# ACCEPTANCE_TESTS.md

# AI Story Co-Author v0.1.1 — Frozen Acceptance

## Verification Rule

每项结果区分 `Engineering Verification` 与 `Real-LLM Semantic Verification`。Mock 不得替代语义验收。

### AC-101 Planner Continuation — REAL LLM
故事已穿越、认识艾琳、入住。新 Stage：第二天和艾琳去冒险者公会。PASS：从当前状态继续；FAIL：再次穿越/初遇/找住所。

### AC-102 Writer Context Wiring — ENGINEERING
数据库已有 State/Memory/Relationship 时，捕获 `GenerateChapterRequest`。相关字段必须真实非空。

### AC-103 Chapter Length — REAL LLM
`targetCharacters=3000`，连续 5 章，至少 4 章在 2250–3750，且无明显重复灌水。

### AC-104 Chapter Goal — REAL LLM
ChapterSpec：进入公会并登记；Must Advance=到达/登记/测试铺垫；Must Not=不暴露天帝身份。正文必须实际遵循。

### AC-105 Low-value Detail Isolation — REAL LLM
普通面包为 `TRANSIENT_DETAIL, importance=1, scope=CHAPTER`。后续 3 个无关 ChapterSpec 不得持续围绕面包。

### AC-106 Replan Remaining — ENGINEERING + REAL LLM
9 章计划完成 1–3 后，将剩余改为 3 章。PASS：1–3 不变，旧剩余 superseded，新 4–6，无重复，新计划从当前剧情继续。

### AC-107 Manual Edit — ENGINEERING
AI Draft 有“获得铁剑”，作者删除并保存。新 Revision 生效，旧 Revision 保留，Chapter ID/Number 不变。

### AC-108 Memory Refresh — ENGINEERING
承接 AC-107。旧 extraction stale/inactive，re-extract 后铁剑不再是正式 Current State。

### AC-109 Polish Fact Preservation — REAL LLM
要求减少解释性语言、增强自然对话。核心事件、地点、正式物品、Ending Intent 不变。

### AC-110 Extraction Failure Recovery — ENGINEERING
Writer 成功、Chapter 已保存、Extractor 首次失败。Retry 必须先重试同章 Extraction，再进入下一 Plan。

### AC-111 Async Continuous Progress — ENGINEERING
Start API 快速返回 Job；后台继续；GET Job 可观察 1/N → 2/N；Start 不阻塞整批完成。

### AC-112 Pause — ENGINEERING
Pause 请求后，当前单章完成安全 checkpoint，Job=PAUSED，不继续下一章。

### AC-113 Stage Completion — ENGINEERING
active plans 全部完成后 `GenerationJob=COMPLETED` 且 `Stage=COMPLETED`。

### AC-114 600 Chapter Pace — REAL LLM
`target=600, current=5, Arc=1–60 生存与融入`。不得主动规划最终真相、终局大战、最终回归通道或全书主矛盾解决。

### AC-115 Memory Dedup — ENGINEERING
同一长期事实连续重复确认，不产生大量等价 StoryMemory。

### AC-116 Inventory Multi-item — ENGINEERING
先获得铁剑，再获得药水，两者同时保留，不互相覆盖。

### AC-117 Mock Boundary
Mock 只能证明 HTTP、DTO、persistence、parsing、context wiring、retry、workflow、state machine。用 Mock 宣称文风、续写语义、Memory 改善正文、长篇节奏或 Polish 质量的验收无效。

## Required Metrics

真实 LLM Run 至少记录：Model、Prompt Version、Story ID、Run ID、Target/Actual Characters、Length Pass Rate、Chapter Goal Completion Rate、Continuation Failure Count、Low-value Detail Repetition Count、Duplicate Memory Count、Manual Edit/Regeneration/Polish Count、Memory Extraction Failure Count、Average Generation Latency。

## Final Acceptance

Engineering suite PASS，且 AC-101/103/104/105/106/109/114 真实 LLM PASS 并保留证据后，才能声明 `v0.1.1 ACCEPTED`。
