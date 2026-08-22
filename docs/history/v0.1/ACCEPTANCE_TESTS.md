# ACCEPTANCE_TESTS.md

## 1. Document Purpose

本文档定义 **AI Story Co-Author v0.1** 的验收方法。

它回答：

- 什么情况下可以认为某项功能已经完成；
- 如何验证 Stage Planning、Chapter Generation、Memory、Planner、Story Query 等能力；
- 如何判断 Story Memory 是否实际帮助了连续创作；
- 如何区分产品 Bug、Memory Bug 和 LLM 本身的生成问题；
- 什么情况下允许宣布 v0.1 完成。

本文档不是完整自动化测试规范。

它包含：

- Product Acceptance Tests；
- AI Behavior Tests；
- Core Hypothesis Experiments；
- Manual Evaluation。

具体 Unit Test、Integration Test、API Test 和测试框架由后续技术文档和实现计划定义。

---

# 2. Acceptance Philosophy

v0.1 不以以下标准作为成功依据：

> “感觉生成得不错。”

> “看起来挺智能。”

> “AI 好像记住了。”

AI 系统存在随机性，因此核心行为必须尽可能使用：

```text
预先设定事实
+
预先设定预期结果
+
重复测试
```

进行验证。

对于无法完全自动判断的创作质量，允许人工评估。

但必须明确评估对象。

---

# 3. v0.1 Core Question

v0.1 最重要的实验问题是：

> **作者只提供阶段性剧情方向时，AI 是否能够利用 Story Constraints、Current State 和 Story Memory，连续完成多个相关章节，而不要求作者逐章重新提供完整背景？**

v0.1 不负责证明：

- 100 章以上长期稳定；
- 小说文学质量达到出版水平；
- 当前 Memory 架构是最终最优方案；
- RAG 一定需要或一定不需要；
- AI 可以完全取代作者。

---

# 4. Test Environment Principle

正式验收应使用一个固定测试故事。

避免每次测试都随机更换故事设定，否则难以比较结果。

建议保留一个：

> **Acceptance Story**

专门用于 v0.1 测试。

---

# 5. Acceptance Story

建议固定使用以下基础故事。

## 5.1 Core Idea

> 一名修仙世界的天帝意外穿越到西幻魔法世界。  
> 他保留部分修仙能力，但不了解这个世界的魔法体系和社会规则。

---

## 5.2 Initial Stage Direction

> 主角暂时隐藏真实身份，先了解这个陌生世界，并尝试融入当地社会。

---

## 5.3 Initial Story Constraints

为了方便测试，预先规定以下规则。

### Constraint C1 — Narrative Perspective

全文使用第三人称。

### Constraint C2 — Writing Style

整体风格以轻松、爽文式网文为主。

### Constraint C3 — Magic Knowledge

主角一开始不了解西幻世界的魔法体系。

### Constraint C4 — Identity

当地人不知道主角原本是修仙世界天帝。

### Constraint C5 — World Rule

当地普通人普遍认为力量体系建立在魔力基础之上。

这些规则用于测试：

> Story Constraints 是否在跨章节生成中保持稳定。

---

# 6. Seeded Memory Facts

为了测试 Memory，不完全依赖 AI 自由生成。

在测试故事中故意加入一些以后容易被遗忘的信息。

---

## 6.1 Character State S1

Chapter 1 中明确发生：

> 主角右肩在穿越过程中受伤。

Expected Current State：

```text
Right Shoulder:
Injured
```

在没有治疗事件前：

> 后续章节不得无原因表现为完全正常。

---

## 6.2 Inventory State S2

Chapter 1：

> 主角身上只保留了一枚来自原世界的玉佩。

Expected Current State：

```text
Inventory:
Ancient Jade Pendant
```

---

## 6.3 Relationship State S3

Chapter 2：

> 当地女孩艾琳帮助主角找到临时住处，但仍然对他的身份保持警惕。

Expected Relationship：

```text
艾琳 -> 主角

Friendly / Helpful
but
Still Suspicious
```

系统不得直接把关系解释成：

```text
Close Friend
```

或：

```text
Romantic Interest
```

除非后续正文提供足够依据。

---

## 6.4 Detail Memory M1

Chapter 1：

> 主角的玉佩在穿越完成后短暂发热了一次。

正文不解释原因。

Expected：

> 系统至少能够识别它可能具有长期价值。

作者应能够将其保存为 Story Memory。

---

## 6.5 Potential Foreshadowing M2

Chapter 2：

> 一名路过的黑袍老人看到主角玉佩后停顿了一瞬，但什么都没有说。

Expected：

AI 可以建议：

```text
REVIEW
Potential Foreshadowing
```

但不得自动宣布：

> 黑袍老人知道主角真实身份。

---

# 7. Test Categories

v0.1 验收分为：

```text
A. Core Workflow

B. Stage Planning

C. Chapter Generation

D. Story Constraints

E. Current State

F. Story Memory

G. Memory Extraction

H. Memory Review

I. Source Evidence

J. Story Query

K. Planner Suggestions

L. Generation Modes

M. Failure Handling

N. Core Memory Experiment
```

---

# 8. A — Core Workflow Tests

## AT-A01 — Create Story

### Steps

1. 打开 Web UI。
2. 创建 Acceptance Story。
3. 输入 Core Idea。
4. 输入 Story Constraints。
5. 输入 Initial Stage Direction。

### PASS

系统成功保存 Story Project。

重新进入 Story 后，以上信息仍然存在。

### FAIL

出现以下任意情况：

- Story 无法保存；
- 刷新后数据丢失；
- Constraints 被 AI 自动修改；
- Stage Direction 与其他 Story 混淆。

---

## AT-A02 — Complete One Stage

### Steps

1. 输入 Stage Direction。
2. Planner 生成章节计划。
3. 作者接受计划。
4. Writer 生成全部计划章节。
5. 每章完成 Memory Extraction。
6. Stage 最终进入 Completed。

### PASS

完整流程无人工重新输入背景即可结束。

---

# 9. B — Stage Planning Tests

## AT-B01 — Suggested Chapter Count

输入：

> 主角和艾琳前往冒险者公会完成注册，并在过程中第一次小规模展示自己的特殊力量。

### PASS

Planner：

- 给出明确建议章节数；
- 给出对应数量 Chapter Goals；
- 每章有明显不同推进作用。

### FAIL

例如：

- 没有明确章节数；
- 5 个 Chapter Goal 基本重复；
- Planner 直接生成正文；
- 计划明显偏离 Stage Direction。

---

## AT-B02 — Compress Plan

### Steps

假设 Planner 建议：

```text
5 Chapters
```

作者修改为：

```text
2 Chapters
```

### PASS

Planner 重新规划为 2 章。

两章仍然覆盖 Stage Direction 的核心目标。

### FAIL

只是删除后三章，导致目标无法完成。

---

## AT-B03 — Author Edits Chapter Goal

作者修改其中一章：

> 不允许主角在这一章暴露真实修仙身份。

### PASS

重新确认计划后：

Writer 使用修改后的目标。

---

# 10. C — Chapter Generation Tests

## AT-C01 — Generate Planned Chapter

Writer 得到：

```text
Chapter Goal:
主角与艾琳抵达冒险者公会，
了解注册流程。
```

### PASS

章节主要推进该目标。

### FAIL

Writer：

- 完全跳过公会；
- 擅自进入完全无关剧情；
- 自行把 Stage 扩大到其他主线。

---

## AT-C02 — No Repeated Full Background

生成第 4、5 章时：

作者不重新输入：

- 主角身份；
- 艾琳是谁；
- 当前地点；
- 玉佩；
- 世界规则。

### PASS

系统仍能利用已有 Story 信息完成生成。

---

# 11. D — Story Constraint Tests

## AT-D01 — Narrative Perspective

连续生成至少 5 章。

### PASS

没有无理由长期切换成第一人称。

---

## AT-D02 — Identity Constraint

已确定：

> 当地人不知道主角是修仙世界天帝。

### PASS

普通 NPC 不会无来源直接知道：

> “你就是另一个世界的天帝。”

---

## AT-D03 — Magic Knowledge

前期主角尚未学习本世界魔法体系。

### PASS

主角不得突然熟练使用当地魔法理论和术语。

除非正文已经产生学习过程。

---

# 12. E — Current State Tests

## AT-E01 — Injury Persistence

Chapter 1：

> 主角右肩受伤。

随后没有治疗事件。

### PASS

后续 Writer 接收到：

```text
Right Shoulder:
Injured
```

正文没有明显表现为完全没有受伤。

---

## AT-E02 — State Change

后续正文明确：

> 艾琳使用治疗魔法治愈主角右肩。

Memory Extraction 应产生状态变化。

Expected：

```text
Right Shoulder:
Recovered
```

### PASS

之后 Writer 不再使用：

```text
Right Shoulder:
Injured
```

作为 Current State。

---

## AT-E03 — Inventory Persistence

Chapter 1：

> 主角拥有玉佩。

未发生遗失事件。

### PASS

Story Query 可以回答：

> 主角目前仍然拥有玉佩。

---

## AT-E04 — Inventory Removal

后续人为安排：

> 玉佩被黑袍老人偷走。

### PASS

之后 Current State 不再显示：

```text
主角当前持有玉佩
```

---

# 13. F — Story Memory Tests

## AT-F01 — Preserve Early Detail

Chapter 1：

> 玉佩曾短暂发热。

Chapter 5 查询：

> 玉佩之前有没有出现过异常？

### PASS

系统能够找到：

> Chapter 1 曾发生异常发热。

作者无需重新输入该信息。

---

## AT-F02 — Foreshadowing Preservation

Chapter 2：

> 黑袍老人观察玉佩。

作者确认保存为 Foreshadowing。

Chapter 5 查询：

> 当前有哪些可用伏笔？

### PASS

该信息仍然存在。

---

## AT-F03 — Do Not Invent Foreshadowing

正文只写：

> 酒馆老板擦了擦桌子。

### PASS

Memory Agent 不应无依据高置信度标记：

> 酒馆老板可能是隐藏 Boss。

---

# 14. G — Memory Extraction Tests

## AT-G01 — Explicit Location Change

正文：

> 主角和艾琳离开酒馆，来到冒险者公会。

### Expected Candidate

```text
Type:
Current State

Subject:
主角

Field:
Location

Value:
冒险者公会

Suggested Action:
AUTO
```

### PASS

Location 被正确识别。

---

## AT-G02 — Explicit Item Acquisition

正文：

> 主角购买一把普通铁剑。

Expected：

```text
Inventory:
Add Iron Sword

AUTO
```

---

## AT-G03 — Relationship Interpretation Boundary

正文：

> 艾琳仍然抱怨主角，但已经不再要求他离开。

合理 Candidate：

> 艾琳对主角的敌意有所降低。

不合理 Candidate：

> 艾琳爱上了主角。

### PASS

Extraction 没有明显超出正文证据。

---

## AT-G04 — High Impact World Rule

正文：

> 有人猜测主角的力量可能完全不依赖魔力。

Expected：

```text
REVIEW
```

不得直接成为确定世界规则：

> 修仙力量完全无视魔法体系。

---

# 15. H — Memory Review Tests

## AT-H01 — AUTO Override

AI：

```text
AUTO
```

作者改为：

```text
IGNORE
```

### PASS

该 Candidate 不进入有效 Memory。

---

## AT-H02 — IGNORE Override

AI 将某个细节判断：

```text
IGNORE
```

作者选择：

```text
Save as Story Memory
```

### PASS

信息成功保存。

---

## AT-H03 — REVIEW Edit

AI 提取：

> 艾琳喜欢主角。

作者修改：

> 艾琳对主角的警惕有所降低。

### PASS

最终保存作者修改后的内容。

不得同时保存 AI 原始错误版本作为有效事实。

---

# 16. I — Source Evidence Tests

## AT-I01 — Memory Has Source Chapter

任意从正文产生的 Memory。

### PASS

能够查看：

```text
Source Chapter
```

---

## AT-I02 — Evidence Text

Memory：

> 艾琳对主角敌意降低。

### PASS

能够显示支持该判断的正文片段。

---

## AT-I03 — Evidence Contradicts Memory

人工制造一个错误 Memory：

> 艾琳深爱主角。

但 Evidence 只显示：

> 她不再要求主角离开。

### PASS

作者能够通过 UI 修改或删除错误 Memory。

系统不得阻止作者纠正 AI。

---

# 17. J — Story Query Tests

## AT-J01 — Current Location

Question：

> 主角现在在哪里？

### PASS

回答：

> 当前位于冒险者公会。

不得返回已经离开的酒馆作为 Current Location。

---

## AT-J02 — Current Inventory

Question：

> 主角现在有哪些重要物品？

### PASS

根据 Current State 返回。

---

## AT-J03 — Relationship Query

Question：

> 艾琳和主角现在是什么关系？

### PASS

回答基于当前有效 Relationship Memory。

---

## AT-J04 — Foreshadowing Query

Question：

> 当前有哪些还没使用的伏笔？

### PASS

至少返回作者已确认的 Open / Active Foreshadowing。

---

## AT-J05 — Unknown Answer

Question：

> 主角的父亲叫什么？

如果正文和 Memory 从未定义：

### PASS

回答类似：

> 当前故事没有确定这一信息。

### FAIL

AI自行创造一个父亲姓名。

---

# 18. K — Planner Suggestion Tests

## AT-K01 — Three Different Directions

作者请求：

> 帮我想下一步剧情。

### PASS

至少提供 3 个有明显差异的方案。

---

## AT-K02 — Use Existing Story

如果已有：

> 黑袍老人注意玉佩。

至少允许 Planner 在某个合理方案中利用该已有伏笔。

但不强制所有方案都围绕该伏笔。

---

## AT-K03 — Does Not Modify Story Automatically

Planner 给出方案。

### PASS

在作者点击接受之前：

- Current Stage 不改变；
- Story Memory 不改变；
- Chapter 不生成。

---

# 19. L — Generation Mode Tests

## AT-L01 — Step-by-Step

设置：

```text
3 Chapter Stage
Step-by-Step
```

### PASS

每章生成后暂停。

作者必须主动继续下一章。

---

## AT-L02 — Continuous

设置：

```text
3 Chapter Stage
Continuous
```

### PASS

系统自动完成：

```text
Chapter 1
→
Memory
→
Chapter 2
→
Memory
→
Chapter 3
```

不要求用户逐章重新点击生成。

---

# 20. M — Failure Handling Tests

## AT-M01 — Writer Failure

人为模拟一次 LLM 调用失败。

### PASS

显示明确失败状态。

允许 Retry。

不得产生空 Chapter 并标记 Completed。

---

## AT-M02 — Memory Extraction Failure

Chapter 已经成功生成。

Memory Extraction 失败。

### PASS

Chapter 保留。

状态明确显示：

```text
Memory Extraction Failed
```

允许重新执行。

---

## AT-M03 — Planner Failure

Planner 返回无法解析结果。

### PASS

Stage 不进入 Running。

用户可以 Retry。

---

# 21. N — Core Memory Experiment

这是 v0.1 最重要的实验之一。

目标：

> 初步验证 Structured Story Memory 是否对连续创作产生实际帮助。

---

# 22. Experiment N1 — Baseline vs Memory

使用相同：

- Core Idea；
- Story Constraints；
- Stage Direction；
- 模型；
- 尽可能相同的生成参数。

分别运行两个版本。

---

## Version A — Baseline

Writer 只获得：

```text
Story Constraints
+
Stage Direction
+
Current Chapter Goal
+
近期章节内容 / Summary
```

不提供 Structured Current State 和 Story Memory。

---

## Version B — Memory

Writer 获得：

```text
Story Constraints
+
Stage Direction
+
Current Chapter Goal
+
Current State
+
Story Memory
+
近期章节内容 / Summary
```

---

# 23. Experiment Seed Facts

两个版本都在早期章节植入相同信息。

例如：

### Fact 1

主角右肩受伤。

### Fact 2

主角拥有玉佩。

### Fact 3

艾琳仍然怀疑主角身份。

### Fact 4

玉佩曾经异常发热。

### Fact 5

黑袍老人注意过玉佩。

---

# 24. Experiment Evaluation

至少观察后续章节是否出现：

### Constraint Violation

违反世界或人物硬设定。

### State Error

例如：

> 伤势未恢复却完全消失。

### Inventory Error

例如：

> 已丢失物品继续使用。

### Relationship Error

例如：

> 艾琳突然表现为多年好友。

### Detail Forgetting

重要早期细节完全无法在需要时使用。

### Foreshadowing Forgetting

已有伏笔在 Planner / Query 中无法找到。

---

# 25. Experiment Recording

每次实验至少记录：

```text
Run ID

Model

Generation Mode

Memory Enabled:
YES / NO

Number of Chapters

Observed Errors

Manual Corrections Required

Notes
```

不需要第一版建设专业 Evaluation Platform。

简单测试记录即可。

---

# 26. Experiment Interpretation

如果 Memory Version 明显优于 Baseline：

> 支持继续研究 Structured Memory。

如果没有明显改善：

不能直接得出：

> Memory 没用。

需要进一步区分：

### Case A

Memory 本身提取错误。

### Case B

正确 Memory 没有被提供给 Writer。

### Case C

Memory 已提供，但 Writer 不遵守。

### Case D

测试规模过小。

### Case E

近期上下文本身已经足以解决问题。

这些结果都属于有效实验发现。

---

# 27. Five-Chapter End-to-End Acceptance

v0.1 最终必须完成一个至少 5 章的完整 Story Project。

---

## Required Flow

```text
Create Story
↓
Enter Stage Direction
↓
Planner Creates Plan
↓
Author Confirms
↓
Generate Chapters
↓
Memory Extraction
↓
Current State Update
↓
Story Memory Update
↓
Continue Generation
↓
Complete Stage
↓
Story Query
↓
Planner Suggestions
```

---

# 28. Five-Chapter Required Observations

5 章测试至少包含：

- 1 个持续身体状态；
- 1 个物品状态；
- 1 个角色关系状态；
- 1 个跨章节重要细节；
- 1 个潜在伏笔；
- 1 次明确状态变化。

---

# 29. Five-Chapter PASS Criteria

v0.1 最终验收至少满足：

### PASS-01

成功生成 5 个逻辑连续章节。

### PASS-02

作者没有逐章重新输入完整 Story Background。

### PASS-03

至少一次 Stage Direction 自动扩展为多个章节。

### PASS-04

Current State 实际参与后续 Chapter Generation。

### PASS-05

至少一个早期 Story Memory 在较晚章节或 Query 中被正确使用。

### PASS-06

至少一次 Memory State Change 正确更新 Current State。

### PASS-07

至少一个 REVIEW Candidate 被作者确认或修改。

### PASS-08

至少一个 IGNORE / AUTO 分类被作者成功覆盖。

### PASS-09

至少一条 Memory 能追溯到 Source Chapter 和 Evidence。

### PASS-10

Story Query 能正确回答基础状态问题。

### PASS-11

Planner 能提出 3 个不同的后续剧情方向。

### PASS-12

Continuous 和 Step-by-Step Mode 均至少成功运行一次。

---

# 30. What Does NOT Automatically Fail v0.1

以下问题出现时，不自动判定整个 v0.1 失败：

- 某一章文学质量不高；
- Planner 某个建议不好；
- 某条 Memory 被错误分类；
- 某个情绪转换不够自然；
- AI 偶尔需要 Retry；
- Memory 仍存在一定重复；
- 尚未实现 RAG；
- 尚未实现 Vector Search；
- 尚未实现完整 Temporal Memory；
- 尚未实现 Canon；
- 5 章测试不能证明 100 章可靠。

这些问题应该进入：

```text
KNOWN_ISSUES
/
DEBT
/
NEXT_VERSION
```

而不是无限延长 v0.1。

---

# 31. Critical Failure Conditions

以下情况意味着 v0.1 核心假设尚未形成可用闭环：

### Critical-01

无法连续生成多个相关章节。

### Critical-02

后续章节完全无法使用已有 Story State / Memory。

### Critical-03

每章都必须由作者重新输入大量背景。

### Critical-04

Memory Extraction 大量产生与正文无关的虚构事实。

### Critical-05

Current State 更新后，Writer 仍持续大量使用旧状态。

### Critical-06

Continuous Mode 无法在无人逐章操作的情况下运行。

如果出现这些问题：

> 优先修复核心闭环，而不是增加新功能。

---

# 32. Manual Review Questions

完成 5 章测试后，开发者应回答：

1. 如果没有 Memory，这 5 章是否明显更容易产生状态错误？
2. Memory Agent 保存了多少真正有用的信息？
3. 保存了多少无价值信息？
4. REVIEW 是否让作者产生明显负担？
5. Writer 是否真正使用了 Memory？
6. Planner 是否降低了作者规划中间过程的工作量？
7. 作者需要几次手工纠正？
8. 哪一种 Memory 类型最有价值？
9. 哪一种设计看起来没有实际价值？
10. 当前最大的真实失败模式是什么？

这些答案将决定 v0.2，而不是原有宏大 PRD 决定 v0.2。

---

# 33. Acceptance Evidence

v0.1 完成时，应保留以下证据：

- Acceptance Story；
- 5 个连续章节；
- Stage Plan；
- Story Constraints；
- Current State 示例；
- Story Memory 示例；
- REVIEW 示例；
- Source Evidence 示例；
- Story Query 示例；
- Planner Suggestions；
- Baseline vs Memory 测试结果；
- 已知问题列表。

这些证据未来可用于：

- README；
- GitHub 项目说明；
- 简历项目描述；
- 面试演示；
- v0.2 设计决策。

---

# 34. Definition of Acceptance Complete

当：

```text
核心产品闭环完成
+
5章端到端测试通过
+
主要 Memory 行为可观察
+
Baseline / Memory 实验至少执行一次
+
失败和不足有明确记录
```

即可宣布：

> **AI Story Co-Author v0.1 — Accepted**

Acceptance Complete 不意味着产品已经成熟。

它意味着：

> 已经获得足够真实的运行结果，可以决定下一步应该继续什么、修改什么、删除什么。