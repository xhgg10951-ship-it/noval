# PRODUCT_SPEC.md

## 1. Document Purpose

本文档定义 **AI Story Co-Author v0.1** 的具体产品行为。

它回答：

- 用户在产品中可以做什么；
- 系统在每一步应该做什么；
- Planner、Writer、Memory、Story Query 分别产生什么结果；
- 用户如何控制 AI；
- 哪些结果需要确认；
- 连续生成如何运行；
- 系统出现异常或无法安全继续时应该如何表现。

本文档不定义：

- 数据库表结构；
- Java 类结构；
- Spring Boot API；
- MyBatis Mapper；
- Python 模块结构；
- LangChain Chain / Agent 具体实现；
- Prompt 文本；
- 模型 Provider；
- Vector Database；
- 部署方式。

以上实现细节由后续 `TECH_STACK.md`、`ARCHITECTURE.md` 和实际开发决定。

---

# 2. Product Definition

v0.1 是一个面向单个小说作者的 AI Co-Author Web 应用。

产品的核心协作模式是：

> **作者负责创意、阶段方向和关键判断，AI 负责规划中间过程、生成章节、维护故事信息，并在作者需要时提供剧情建议和作品查询能力。**

系统不要求作者为每一章重新提供：

- 世界观；
- 人物信息；
- 已发生剧情；
- 当前角色状态；
- 已确认的重要 Memory。

---

# 3. Primary Product Loop

v0.1 的核心循环为：

```text
创建故事
↓
作者输入当前阶段方向
↓
Planner 拆分章节计划
↓
作者接受或调整
↓
选择生成模式
↓
Writer 生成章节
↓
Memory Extraction
↓
Memory Update / Review
↓
下一章节
↓
阶段完成
↓
作者提供下一阶段方向
```

整个产品必须围绕这个循环设计。

任何无法直接帮助该循环的能力默认不属于 v0.1 核心功能。

---

# 4. Story Project

## 4.1 Create Story

用户可以创建一个新的 Story Project。

创建时至少可以提供：

### Required

- Story Name
- Core Idea

### Optional but Recommended

- Genre
- Writing Style
- Narrative Perspective
- Main Character
- Initial World Setting
- Hard Constraints
- Initial Stage Direction

---

## 4.2 Core Idea

Core Idea 表示整个作品最初的核心爆点。

例如：

> 修仙界天帝意外穿越到西幻魔法世界。

Core Idea 不要求详细。

用户可以只提供一句或者几句话。

系统不应要求用户在创建项目时提前设计完整世界观和所有角色。

---

## 4.3 Story Constraints

用户可以填写长期创作约束。

例如：

```text
第三人称限知视角。

整体文风偏轻松爽文。

主角不会主动滥杀无辜。

死者不能通过普通魔法复活。

禁止现代网络热梗。
```

Story Constraints 应能够：

- 在后续创作中持续使用；
- 被用户查看；
- 被用户修改；
- 明确区别于 AI 自动生成的普通剧情内容。

v0.1 不要求复杂版本管理。

---

# 5. Stage Direction

## 5.1 Definition

Stage Direction 表示：

> 作者当前希望故事“接下来大致往哪里走”。

它不是完整章节 Prompt。

例如：

> 主角刚刚穿越，对这个世界完全不了解。接下来先让他遇到当地人，并逐渐了解这个世界和魔法体系。

或者：

> 接下来主角和艾琳去冒险者公会注册，并安排一次人前显圣。

作者不需要描述：

- 每章发生什么；
- 每个场景怎么展开；
- 每句对白说什么。

这些属于 AI 扩展职责。

---

## 5.2 Create Stage Direction

用户可以：

- 输入新的 Stage Direction；
- 修改尚未执行的 Stage Direction；
- 在上一阶段完成后创建下一阶段；
- 在当前阶段暂停后重新调整方向。

系统应明确区分：

```text
当前执行中的 Stage
已完成 Stage
未来 / 未执行 Stage
```

v0.1 不要求复杂分支剧情管理。

---

# 6. Stage Planning

## 6.1 Planner Input

Planner 至少基于以下信息规划：

- Core Idea
- Story Constraints
- Current Stage Direction
- Current State
- Relevant Story Memory
- Existing story progress

---

## 6.2 Planner Output

Planner 应返回：

### Suggested Chapter Count

例如：

> 建议 4 章完成。

### Chapter Plan

每章至少包括：

- Chapter Order
- Chapter Goal
- Key Expected Progress

例如：

```text
Chapter 4

目标：
主角和艾琳前往冒险者公会。

主要推进：
介绍公会体系；
展示主角对当地职业系统的陌生；
为注册测试制造铺垫。
```

---

## 6.3 Plan Granularity

Planner 不应该生成详细到等同正文的计划。

计划主要描述：

> 本章必须完成什么。

Writer 负责：

> 本章具体怎么写。

---

## 6.4 Author Adjustment

Planner 给出计划后，作者必须可以：

- 接受；
- 修改章节数量；
- 修改章节目标；
- 删除某一章节；
- 要求重新规划。

例如：

```text
AI：
建议5章。

作者：
太慢，压缩成2章。
```

系统重新生成符合 2 章限制的计划。

---

## 6.5 Plan Lock

作者确认计划后，该 Stage Plan 进入执行状态。

Writer 不得因为生成过程中临时产生新想法而自行无限增加章节。

如果计划明显无法继续，系统应：

```text
暂停
↓
说明问题
↓
请求重新规划
```

而不是静默扩大 Stage Scope。

---

# 7. Generation Mode

作者确认 Stage Plan 后，可以选择生成方式。

## 7.1 Continuous Mode

系统连续执行整个 Stage。

流程：

```text
Generate Chapter
↓
Extract Memory
↓
Apply safe Memory updates
↓
Continue
```

直到：

- 所有计划章节完成；
- 用户停止；
- 系统无法安全继续；
- 出现必须人工判断的问题。

---

## 7.2 Step-by-Step Mode

系统每生成一章后暂停。

用户可以：

- 阅读章节；
- 查看 Memory Extraction；
- 修改下一章方向；
- 继续；
- 停止当前 Stage。

---

## 7.3 Mode Switching

如果实现复杂度允许，用户 SHOULD 能在 Stage 过程中从：

```text
Continuous
→
Step-by-Step
```

切换。

反方向切换属于可选能力。

v0.1 不应为了模式切换设计复杂 Workflow Engine。

---

# 8. Chapter Generation

## 8.1 Writer Responsibility

Writer 的主要职责：

> 根据已确认的 Chapter Goal 生成完整章节正文。

Writer 不负责重新定义整个产品方向。

---

## 8.2 Writer Context

Writer 至少应获得：

- Core Idea
- Story Constraints
- Current Stage Direction
- Current Chapter Goal
- Current State
- Relevant Story Memory
- 必要的近期剧情内容

具体 Context 构造方式由后续实现决定。

---

## 8.3 Chapter Output

每次生成至少返回：

- Chapter Title
- Chapter Content
- Chapter Summary

Chapter Summary 主要用于后续系统理解近期剧情。

---

## 8.4 Chapter Length

v0.1 不要求严格达到正式网文章节标准。

为了开发和测试效率：

> 测试阶段允许生成短章节。

产品 UI SHOULD 允许后续调整目标章节长度。

正式字数目标不属于 v0.1 核心验收标准。

---

# 9. Writer Boundaries

Writer MUST NOT 在没有充分依据时擅自创造高影响长期设定。

尤其包括：

- 主角突然新增核心家庭成员；
- 修改已有世界核心规则；
- 修改角色核心身份；
- 给主角增加永久重大能力；
- 推翻作者明确的 Story Constraints。

如果 Writer 产生了这样的内容：

Memory 系统应至少能够将其识别为需要 Review 的候选。

v0.1 不要求自动删除或自动重写正文。

---

# 10. Memory Extraction

## 10.1 Trigger

每个 Chapter 成功生成后，必须执行一次 Memory Extraction。

---

## 10.2 Goal

Memory Extraction 的目标不是总结整章。

而是识别：

> 哪些新信息可能影响未来创作。

---

## 10.3 Candidate Types

v0.1 至少尝试识别以下类型。

### Current State

- Location
- Emotion
- Physical Condition
- Inventory
- Current Goal
- Relationship State

### Story Event

- 重要事件；
- 战斗结果；
- 新人物相遇；
- 重大决定；
- 任务完成或失败。

### Story Memory

- 重要细节；
- 可能的伏笔；
- 人物秘密；
- 未解决异常；
- 重要承诺。

### Story Setting

- 新世界规则；
- 新能力规则；
- 核心人物背景；
- 重大长期设定。

---

# 11. Memory Candidate

每个 Candidate SHOULD 至少包括：

```text
Type

Subject

Description / Value

Suggested Action

Source Chapter

Evidence Text
```

例如：

```text
Type:
RELATIONSHIP

Subject:
艾琳 -> 林凡

Description:
艾琳对林凡的敌意降低。

Suggested Action:
AUTO

Source:
Chapter 3

Evidence:
艾琳虽然仍然抱怨，但已经主动把剑递给林凡，
并没有再次要求他离开。
```

---

# 12. Memory Classification

v0.1 使用三个默认分类：

```text
AUTO
REVIEW
IGNORE
```

它们表示 AI 的建议行为。

不是不可修改的权限等级。

---

## 12.1 AUTO

表示：

> AI 认为该信息明确、低风险，并适合默认加入 Memory。

典型情况：

- 明确地点变化；
- 明确物品获得或失去；
- 明确身体状态；
- 已发生的重要事件；
- 文本证据充分的关系变化；
- 明确的人物认知变化。

示例：

```text
主角进入冒险者公会。
```

可自动形成：

```text
Current Location:
冒险者公会
```

---

## 12.2 REVIEW

表示：

> 信息可能具有较大长期影响，需要作者判断。

典型情况：

- 新世界规则；
- 主角核心背景；
- 重大能力设定；
- 潜在伏笔；
- 角色长期秘密；
- 高影响关系跃迁；
- AI 强推断内容。

示例：

```text
这一战似乎说明修仙力量完全不受魔法规则影响。
```

系统不应该直接将其变成永久世界规则。

---

## 12.3 IGNORE

表示：

> AI 判断目前没有必要长期保存。

例如：

- 普通动作；
- 一次性环境描写；
- 无意义对白；
- 对后续没有明显影响的细节。

用户仍然可以选择保存。

---

# 13. Author Memory Control

作者拥有最终 Memory 决定权。

用户可以对 Candidate：

- Accept
- Edit
- Reject / Ignore

并且允许覆盖 AI 分类。

例如：

```text
AI:
IGNORE

作者:
Save as Foreshadowing
```

或者：

```text
AI:
REVIEW

作者:
Ignore
```

---

# 14. AUTO Processing in Continuous Mode

为了保证 Continuous Mode 不被每一章的人工确认打断：

### AUTO

可以自动进入有效 Memory。

### REVIEW

默认进入 Pending Review。

### IGNORE

默认不进入长期 Memory。

---

## 14.1 Pending Review

Pending Review 中的信息可以等待当前 Stage 完成后统一处理。

但 v0.1 需要记录：

> 该 Candidate 尚未获得作者确认。

---

## 14.2 Future Blocking Review

当前 v0.1 不强制实现独立的 Blocking Review 类型。

如果测试发现某些未确认信息会导致下一章节无法安全生成，再考虑后续增加：

```text
BLOCKING_REVIEW
```

不得提前构建复杂治理系统。

---

# 15. Current State

## 15.1 Purpose

Current State 回答：

> 当前故事中的对象“现在是什么状态”。

---

## 15.2 Initial Supported States

v0.1 优先支持：

### Character

- Location
- Emotion
- Physical Condition
- Inventory
- Current Goal

### Relationship

- Character A
- Character B
- Current Relationship Description

---

## 15.3 State Update

当新 Chapter 中出现明确状态变化时：

旧状态不能继续作为当前状态使用。

例如：

```text
Chapter 1:
主角拥有木剑。

Chapter 3:
木剑损坏并被丢弃。
```

Chapter 4 Writer 不应该仍然收到：

```text
当前物品：
木剑
```

v0.1 可以先使用简单 Current Value 模型解决。

完整历史状态和 Temporal Memory 不属于当前要求。

---

# 16. Story Memory

## 16.1 Important Events

系统能够保存未来仍可能有意义的历史事件。

---

## 16.2 Important Details

系统可以保存：

> 当时很小，但可能对以后有价值的信息。

---

## 16.3 Foreshadowing

系统可以识别：

> 可能具有伏笔性质的信息。

AI 不应该把所有异常细节直接认定为正式伏笔。

因此潜在伏笔默认适合进入：

```text
REVIEW
```

作者可以确认：

```text
保存为伏笔
```

---

## 16.4 Foreshadowing Status

v0.1 可以只支持简单状态：

```text
OPEN
RESOLVED
```

如果实现复杂度明显增加，则允许暂时只存：

```text
FORESHADOWING
```

而不实现完整生命周期。

---

# 17. Source Evidence

每条从正文提取的 Memory SHOULD 至少能够关联：

```text
source_chapter
```

并保存：

```text
evidence_text
```

目的：

> 让作者判断 AI 的理解是否正确。

---

## 17.1 Evidence Example

Memory：

> 艾琳对林凡敌意降低。

用户查看来源时应能够看到：

```text
Chapter 3

艾琳沉默地把剑递给林凡。
虽然嘴上仍然抱怨，但这次没有再要求林凡离开。
```

---

## 17.2 Evidence Limitation

Evidence Text 不是新的事实来源。

正文仍然是最终原始内容。

---

# 18. Planner Suggestion Mode

除了 Stage Breakdown，Planner 还必须支持：

> 下一步剧情建议。

---

## 18.1 User Trigger

用户主动点击类似：

```text
帮我想接下来怎么发展
```

---

## 18.2 Planner Suggestion Input

至少使用：

- Current Story Progress
- Story Constraints
- Current State
- Important Story Memory
- Open Foreshadowing if available

---

## 18.3 Output

Planner 返回约 3 个具有明显差异的方向。

不应该只是：

```text
方案1：去冒险
方案2：去新的冒险
方案3：进行一次冒险
```

而应具有不同剧情功能。

例如：

```text
方案 A：
让主角在注册测试中出现异常，
引起公会高层注意。

方案 B：
注册过程正常，
但第一次任务中被迫暴露修仙力量。

方案 C：
因为检测不到魔力而被当成普通人，
先经历轻视，再通过实际行动证明能力。
```

---

## 18.4 User Decision

用户可以：

- 选择某个方案作为新的 Stage Direction；
- 修改方案后采用；
- 全部拒绝；
- 再生成一组。

Planner Suggestions 不自动修改故事方向。

---

# 19. Story Query

## 19.1 Purpose

Story Query 让作者能够像询问助理一样查询自己的作品。

---

## 19.2 Supported Questions

v0.1 优先支持：

### Character State

```text
主角现在在哪里？

主角目前有什么物品？

主角现在身体状态怎么样？
```

### Relationships

```text
主角目前认识哪些重要人物？

艾琳和主角现在是什么关系？
```

### Story Memory

```text
当前有哪些潜在伏笔？

目前有哪些还没解决的重要事情？
```

### Source Questions

```text
为什么系统认为艾琳开始信任主角？

这个设定最早在哪一章出现？
```

---

# 20. Story Query Response

回答应尽量区分：

### Structured Fact

例如：

> 主角当前位置是冒险者公会。

### Memory Interpretation

例如：

> 当前 Memory 记录艾琳对主角的敌意已经降低。

### Evidence

例如：

> 该结论来自 Chapter 3 中艾琳主动提供武器并停止驱赶主角的情节。

如果系统无法确认，应允许回答：

> 当前没有足够信息判断。

不得为了给出完整答案而编造不存在的 Story Memory。

---

# 21. RAG / Raw Story Retrieval

v0.1 不要求完整实现 RAG。

Story Query 首先可以依赖：

- Structured Memory
- Current State
- Source Evidence
- Chapter Summary

如果后续真实测试发现：

> 某些问题必须查看大量历史原文才能回答，

再进入 Vector Retrieval 实验。

---

# 22. Story Reading UI

用户应该能够：

- 查看 Story；
- 查看 Chapter List；
- 阅读 Chapter Content；
- 查看 Chapter Summary；
- 查看该 Chapter 产生的 Memory Candidates。

v0.1 不要求专业正文编辑器。

---

# 23. Memory UI

用户应该能够按至少以下方式查看 Memory：

### Current State

查看当前角色状态。

### Story Memory

查看：

- 重要事件；
- 重要细节；
- 伏笔。

### Review Queue

查看等待作者判断的 Candidate。

---

# 24. Generation Progress

Continuous Mode 中，用户必须能够理解当前系统正在做什么。

至少展示类似状态：

```text
Planning

Generating Chapter 2 / 4

Extracting Memory

Updating Story State

Generating Chapter 3 / 4

Completed
```

不允许：

> 页面长时间只显示“AI 正在思考”。

---

# 25. Stop / Pause

Continuous Mode SHOULD 提供停止能力。

停止后：

- 已经完成并成功保存的 Chapter 保留；
- 当前未完成 Chapter 不应被表现为完成；
- 下一次继续时从明确状态恢复。

v0.1 不要求复杂断点续传机制。

---

# 26. Error Handling

## 26.1 LLM Failure

如果一次 AI 调用失败：

系统应该明确显示失败。

可以允许用户：

```text
Retry
```

不得把错误页面表现为正常生成完成。

---

## 26.2 Memory Extraction Failure

如果 Chapter 已生成，但 Memory Extraction 失败：

Chapter 应保持存在。

系统应标记：

```text
Memory Extraction Failed
```

并允许重新执行 Extraction。

不得静默认为 Memory 已经更新成功。

---

## 26.3 Planning Failure

如果 Planner 无法生成有效计划：

用户可以重新尝试。

系统不应自动进入 Writer。

---

# 27. Data Preservation Principle

任何用户已经确认的重要内容不得因为 AI 重新生成而静默丢失。

尤其包括：

- Story Constraints；
- 已完成 Chapters；
- 用户确认的 Memory；
- 用户明确修改的 Stage Direction。

---

# 28. AI Explainability Boundary

产品需要提供：

> 系统使用了哪些 Story 信息，以及 Memory 来自哪里。

但不要求显示：

- 模型完整内部推理过程；
- Chain-of-Thought；
- 隐藏的模型内部 reasoning。

可解释性的重点是：

```text
输入事实
来源
状态
结果
```

---

# 29. v0.1 Default Workflow

为了减少产品复杂度，v0.1 SHOULD 提供一个推荐默认流程。

建议：

```text
作者输入 Stage Direction

↓

AI建议章节数量

↓

作者确认

↓

默认 Step-by-Step Mode

↓

生成 Chapter

↓

Memory Extraction

↓

作者可查看结果

↓

继续
```

作者可以主动切换：

```text
Continuous Mode
```

默认 Step-by-Step 的目的不是降低自主性。

而是：

> 在 v0.1 测试阶段更容易观察 Writer 和 Memory 的失败模式。

---

# 30. v0.1 Product Pages

具体 UI 可以变化，但产品至少需要能够承载以下页面或功能区域。

## 30.1 Story List

功能：

- 查看已有故事；
- 创建故事。

---

## 30.2 Story Workspace

核心工作区。

至少显示：

- Story 信息；
- 当前 Stage；
- Chapter List；
- 当前正文；
- Generation Control。

---

## 30.3 Planning Panel

功能：

- 输入 Stage Direction；
- 查看 Planner 计划；
- 修改章节数；
- 确认计划。

---

## 30.4 Memory Panel

功能：

- 查看 Current State；
- 查看 Story Memory；
- 查看 Review Queue。

---

## 30.5 Planner Assistant

功能：

> 获取后续剧情建议。

---

## 30.6 Story Query

功能：

> 使用自然语言查询当前故事。

Planner Assistant 和 Story Query 可以共享同一侧边栏或聊天式 UI。

不要求设计成独立页面。

---

# 31. Product Priorities

如果开发过程中发生时间冲突，优先级如下：

```text
1. Story Creation

2. Stage Planning

3. Chapter Generation

4. Current State + Memory

5. Continuous Story Loop

6. Memory Review

7. Story Query

8. Planner Suggestions

9. UI Polish

10. Optional Retrieval Features
```

不能为了美化 UI 或增加 RAG 而牺牲前 6 项。

---

# 32. Product Success Signal

v0.1 的核心成功信号不是：

> “AI生成了一篇看起来不错的小说。”

而是：

> 作者给出一个阶段方向以后，系统能够自己规划并生成多个连续章节，并且后续章节实际使用之前维护的 Story State 和 Memory，而作者不需要每章重新整理背景资料。

---

# 33. Known Product Risks

v0.1 主动承认以下风险。

### Risk 1 — Memory Extraction Error

AI 可能错误理解正文。

缓解方式：

```text
Evidence
+
Author Review
```

---

### Risk 2 — Memory Explosion

AI 可能保存太多无价值信息。

v0.1 使用：

```text
AUTO / REVIEW / IGNORE
```

观察是否足够。

---

### Risk 3 — State Drift

AI 可能无法正确更新 Current State。

通过连续章节测试观察。

---

### Risk 4 — Writer Ignores Memory

即使 Memory 正确，Writer 也可能不遵守。

这需要独立观察：

> 是 Memory 不正确，还是 Writer 没使用正确。

---

### Risk 5 — Poor Planning

Planner 可能生成过于水或过快的章节结构。

作者可以调整章节数量与 Chapter Goals。

---

### Risk 6 — User Review Burden

如果 REVIEW Candidate 太多，作者会重新变成数据管理员。

必须记录并观察这一问题。

---

# 34. Open Questions

以下问题在 v0.1 开发前不强制解决：

- Story Constraints 全部放 System Message 是否最佳；
- Current State 应该保存多少类型；
- Emotion 是否适合简单枚举或自然语言；
- Relationship 应该结构化到什么程度；
- 伏笔是否需要独立生命周期；
- Memory 是否需要 Confidence；
- Memory 是否需要自动去重；
- 是否需要保存完整 Memory 历史；
- Writer 应读取多少最近章节；
- Chapter Summary 是否足够；
- 什么情况下真正需要 RAG；
- Vector Retrieval 对小说创作到底提升多少；
- AUTO / REVIEW / IGNORE 分类是否合理。

这些问题必须通过真实测试逐步回答。

---

# 35. Product Change Rule

开发过程中，如果 AI Agent、开发者或测试结果提出新的产品功能：

必须先判断：

> 它是否阻塞当前 v0.1 核心用户流程？

如果不是：

记录为 Future Idea。

不得直接进入 v0.1。

---

# 36. v0.1 Product Completion

当用户能够完整完成以下流程：

```text
创建故事

↓

提供阶段方向

↓

AI生成章节计划

↓

作者调整 / 接受

↓

生成连续章节

↓

自动提取 Memory

↓

维护 Current State

↓

后续章节使用已有 Memory

↓

作者查询当前 Story 信息

↓

Planner 提供下一步候选方向

↓

作者创建下一阶段
```

并成功完成至少一个约 5 章的测试故事后：

> `PRODUCT_SPEC.md` 定义的 v0.1 核心产品闭环视为完成。

后续发现的新问题进入下一轮实验，而不是继续无限扩展 v0.1。