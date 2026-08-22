# MVP_SCOPE.md

## 1. Document Purpose

本文档定义 **v0.1 的唯一开发范围**。

它回答：

- v0.1 到底要验证什么；
- 哪些能力必须实现；
- 哪些能力可以延后；
- 哪些能力明确禁止进入当前版本；
- 什么情况下可以宣布 v0.1 完成。

本文档只约束 **v0.1**。

它不是长期产品功能清单，也不代表未来版本不会实现当前被排除的能力。

当长期愿景与本文档发生冲突时：

> **v0.1 开发范围以本文档为准。**

开发 Agent 不得因为长期愿景中存在某项能力，就自动将其加入 v0.1。

---

# 2. v0.1 Mission

v0.1 只验证一个核心产品假设：

> **作者只提供阶段性剧情方向时，AI 是否能够利用当前故事状态和长期 Memory，自主完成中间若干连续章节，从而减少作者逐章编写详细 Prompt 的需求。**

v0.1 不尝试证明：

- AI 已经可以完全自主创作长篇小说；
- Memory 问题已经被最终解决；
- 当前 Memory 结构是最佳架构；
- 当前 Agent 分工是最佳方案；
- RAG、Vector Database 或其他高级技术是必要方案。

v0.1 是一个：

> **可运行、可演示、可验证、可继续演进的实验版本。**

---

# 3. Time Boundary

v0.1 的目标开发周期为：

> **7 天，越快形成完整闭环越好。**

因此开发优先级必须遵循：

```text
完整可运行闭环
>
核心功能质量
>
基础用户体验
>
额外技术能力
>
架构炫技
```

如果某项功能会明显威胁核心闭环按期完成，则必须延后。

禁止为了：

- 展示更多技术栈；
- 追求所谓“企业级架构”；
- 预防尚未发生的未来问题；
- 提前支持未来版本；

而扩大当前开发范围。

---

# 4. Primary User

v0.1 的主要用户只有：

> **单个小说作者。**

不考虑：

- 多用户；
- 团队协作；
- 编辑与作者权限；
- 管理员；
- 社区用户；
- 商业客户。

v0.1 可以默认：

> 一个用户在本地或单用户环境中创建和管理自己的故事。

---

# 5. Core User Journey

v0.1 必须能够完整跑通以下流程。

## Step 1 — Create Story

作者创建一个故事，并提供最少必要信息，例如：

- 核心爆点；
- 基础世界方向；
- 主角基础信息；
- 创作文风；
- 必须遵守的硬性设定；
- 当前阶段剧情方向。

例如：

```text
核心创意：
修仙界天帝穿越到西幻魔法世界。

当前阶段方向：
主角不了解当地世界和魔法体系，
决定先隐藏身份并了解这个世界。
```

---

## Step 2 — Stage Planning

系统根据作者提供的当前阶段方向：

1. 判断大约需要多少章节；
2. 生成对应章节计划；
3. 向作者展示计划。

例如：

```text
AI 建议：4 章

Chapter 1
主角穿越并确认环境异常。

Chapter 2
遇见当地居民并获得临时帮助。

Chapter 3
初步了解魔法、城市和社会结构。

Chapter 4
发现冒险者体系，为下一阶段做准备。
```

---

## Step 3 — Author Adjusts Plan

作者可以：

- 接受 AI 建议的章节数量；
- 增加章节数；
- 减少章节数；
- 要求 AI 根据新数量重新压缩或展开计划；
- 修改具体章节目标。

例如：

```text
AI 建议：
5 章

作者：
压缩成 2 章

AI：
重新生成 2 章版本计划
```

计划经作者接受后进入执行状态。

---

## Step 4 — Choose Generation Mode

作者可以选择：

### Continuous Mode

系统连续生成当前阶段规划的全部章节。

流程：

```text
Chapter
↓
Memory Extraction
↓
Memory Update
↓
Next Chapter
```

直到：

- 当前阶段完成；
- 作者主动停止；
- 系统遇到无法安全继续的问题。

### Step-by-Step Mode

每生成一章后暂停。

作者可以：

- 阅读；
- 修改方向；
- 继续；
- 停止当前阶段。

底层生成流程应与 Continuous Mode 尽可能保持一致。

---

## Step 5 — Chapter Generation

Writer 根据至少以下信息生成章节：

```text
Story Constraints
+
Current Stage Plan
+
Current Chapter Goal
+
Current State
+
Relevant Story Memory
+
必要的近期剧情上下文
```

v0.1 不要求 Writer 使用复杂 Retrieval Pipeline。

Writer 的职责是：

> 完成已经确定的章节目标。

Writer 不应在未经作者同意的情况下擅自扩大整个阶段目标。

---

## Step 6 — Memory Extraction

每章完成后，Memory 能力读取新章节并识别：

- 当前状态变化；
- 重要事件；
- 人物关系变化；
- 可能影响未来的重要信息；
- 潜在伏笔；
- 新增长期设定。

提取结果进入：

```text
AUTO
REVIEW
IGNORE
```

三种默认分类。

---

## Step 7 — Memory Review

作者能够查看本章产生的 Memory Candidates。

### AUTO

系统默认保存。

例如：

- 明确地点变化；
- 明确物品变化；
- 明确身体状态变化；
- 已实际发生的重要事件；
- 有充分正文依据的关系变化。

作者仍然可以删除或修改。

### REVIEW

系统不会直接作为确定长期 Memory 应用。

例如：

- 新世界规则；
- 角色核心背景；
- 重大能力设定；
- 潜在伏笔；
- 高影响关系变化；
- AI 自行引入的重要长期设定。

作者可以：

- 接受；
- 修改后接受；
- 忽略。

### IGNORE

系统默认认为无需长期保存。

作者仍然可以手动将其加入 Memory。

---

# 6. Story Information Model

v0.1 只要求支持以下三类故事信息。

## 6.1 Story Constraints

用于描述高优先级、相对稳定的创作要求。

包括：

- 文风；
- 叙事视角；
- 世界基础设定；
- 角色核心设定；
- 作者明确规定的禁止事项。

v0.1 可以使用简单实现。

不要求设计完整 Canon 系统。

---

## 6.2 Current State

至少应能够表达部分常见当前状态。

优先支持：

- 角色当前位置；
- 身体状态；
- 当前情绪；
- 当前物品；
- 角色关系；
- 当前目标。

v0.1 不要求解决所有复杂时间状态问题。

---

## 6.3 Story Memory

用于保存可能在未来重新影响创作的重要信息。

优先支持：

- 重要历史事件；
- 重要细节；
- 伏笔；
- 人物秘密；
- 重要承诺或未完成事项。

普通细节和伏笔暂时可以共享基础存储能力。

v0.1 不要求设计完整伏笔生命周期。

---

# 7. Source Evidence

v0.1 必须区分：

```text
Chapter
=
小说真正写出的原始内容

Memory
=
AI 对小说内容的结构化理解
```

一条由正文提取的 Memory 应至少能够关联到：

```text
source_chapter_id
```

并尽可能保留能够解释该 Memory 来源的：

```text
evidence_text
```

例如：

```text
Memory:
艾琳对林凡的敌意降低

Source:
Chapter 3

Evidence:
对应正文片段
```

目的不是建设复杂 Provenance 系统。

目的只是确保：

> 作者能够知道 AI 为什么得出这条 Memory。

---

# 8. Planner Capability

v0.1 中 Planner 只需要支持两个能力。

## 8.1 Stage Breakdown

作者已经知道接下来要发生什么时：

> 将阶段方向拆成若干章节目标。

---

## 8.2 Direction Suggestions

作者不知道下一步怎么发展时，可以请求 Planner：

> 根据当前故事状态提出约 3 个不同的后续剧情方向。

例如：

```text
方案 A
进入冒险者公会测试时出现异常。

方案 B
第一次任务迫使主角暴露部分力量。

方案 C
主角因为没有魔力被当作普通人，
由此产生新的冲突。
```

Planner 只提供候选。

最终剧情方向由作者决定。

---

# 9. Story Query Capability

v0.1 必须提供一个基础 Story Query 能力。

作者能够使用自然语言询问当前作品信息。

至少验证以下类型：

### Current State

```text
主角现在在哪里？

主角现在有什么物品？

主角目前是什么状态？
```

### Relationship

```text
主角目前认识哪些重要人物？

艾琳和主角现在是什么关系？
```

### Story Memory

```text
当前有哪些可能的伏笔？

目前还有哪些重要事情没有解决？
```

### Source

```text
为什么系统认为艾琳开始信任主角？

这个设定最早在哪一章出现？
```

v0.1 优先利用 Structured Memory 和 Source Evidence 回答。

不要求为了 Story Query 强制加入完整 RAG。

---

# 10. User Interface Scope

v0.1 必须提供一个：

> **简单但完整可用的 Web UI。**

不接受只有 CLI 作为最终演示形态。

UI 重点是能够完成核心流程，而不是追求商业级设计。

至少需要能够完成：

- 创建故事；
- 查看章节；
- 输入当前阶段方向；
- 查看 AI 章节计划；
- 调整章节数量或计划；
- 选择连续生成 / 逐章生成；
- 查看生成进度；
- 查看 Memory；
- 处理 REVIEW Memory；
- 使用 Planner 获取剧情建议；
- 使用 Story Query 提问。

不要求：

- 专业小说编辑器；
- Word 级富文本功能；
- 高级排版；
- 自定义主题系统；
- 移动端完整适配。

---

# 11. Required v0.1 Product Capabilities

以下能力属于 **MUST HAVE**。

缺少其中核心链路时，不得声明 v0.1 完成。

### MUST-01 Story Creation

能够创建至少一个独立故事。

### MUST-02 Stage Direction

作者能够输入或修改当前剧情阶段方向。

### MUST-03 Stage Planning

AI 能将阶段方向拆分为章节计划并建议章节数量。

### MUST-04 Plan Adjustment

作者能够调整章节数量，并让系统重新规划。

### MUST-05 Chapter Generation

AI 能根据当前计划生成小说章节。

### MUST-06 Continuous Generation

支持连续生成整个当前阶段。

### MUST-07 Step-by-Step Generation

支持每章生成后暂停。

### MUST-08 Memory Extraction

每章完成后自动提取 Memory Candidates。

### MUST-09 Memory Classification

Memory Candidates 至少支持：

```text
AUTO
REVIEW
IGNORE
```

### MUST-10 Author Memory Control

作者能够接受、修改、删除或忽略 Memory。

### MUST-11 Current State

系统至少能够维护基础角色状态。

### MUST-12 Story Memory

系统能够保存重要事件、细节与潜在伏笔。

### MUST-13 Source Evidence

正文提取出的 Memory 至少能够追踪到来源章节。

### MUST-14 Planner Suggestions

系统能够提供多个下一步剧情候选方向。

### MUST-15 Story Query

作者能够询问基本作品状态与 Memory。

### MUST-16 Consecutive Story

系统必须能够在同一故事中连续生成多个相关章节，而不是生成互不相关的单章 Demo。

---

# 12. SHOULD HAVE

以下功能只有在 MUST HAVE 核心闭环已经稳定运行后才可以实现。

### SHOULD-01

更好的 Memory 查看和过滤界面。

### SHOULD-02

Memory Evidence 点击后跳转到对应章节。

### SHOULD-03

生成过程中显示更清晰的阶段进度。

### SHOULD-04

作者能够暂停正在进行的连续章节生成。

### SHOULD-05

作者能够在阶段执行过程中修改剩余章节方向。

### SHOULD-06

记录基础 Prompt / Model 调用信息用于调试。

### SHOULD-07

基础错误重试和失败提示。

---

# 13. Optional v0.1.1 Capabilities

以下能力默认不属于 7 天 v0.1。

如果核心闭环提前完成，可以继续实现。

## 13.1 Chapter Chunking

将完整章节拆分成 Chunk。

Chunk 应至少记录：

```text
chunk_id
chapter_id
chunk_index
content
```

---

## 13.2 Embedding

对 Chunk 生成 Embedding。

---

## 13.3 Vector Retrieval

根据语义搜索过去相关正文。

Vector Retrieval 的目的主要是：

> 找到可能相关的历史原文。

它不能取代 Structured Memory 对当前状态的维护。

---

## 13.4 Hybrid Query

Story Query 可以同时使用：

```text
Structured Memory
+
Vector Retrieved Raw Text
```

回答更加模糊的历史问题。

这些能力应被视为：

> **v0.1.1 / 后续增强**

而不是 v0.1 完成条件。

---

# 14. Explicitly Out of Scope

以下能力明确禁止进入 v0.1。

即使开发 Agent 认为它们“更加专业”或“以后肯定需要”，也不得擅自加入。

## Memory

- 完整 Canon Governance；
- Candidate / Active / Canonical / Superseded / Archived 全生命周期；
- 完整 Temporal Truth Engine；
- 复杂 Authority 系统；
- 完整 Perspective Scope；
- Knowledge Graph；
- 高级 Memory Deduplication Framework；
- 固定 Projection Entry Budget；
- 完整 Memory Conflict Resolution。

## Agent

- 自研通用 Multi-Agent Framework；
- 动态创建 Agent；
- Agent 自我修改；
- Agent 自主扩大产品目标；
- Level 10 全自主 Agent；
- 复杂 Agent-to-Agent 协商协议。

## AI Infrastructure

- 多模型自动路由；
- 多 Provider 故障切换；
- 自研 Embedding 模型；
- 自研 Vector Database；
- 复杂 Context Compression；
- 高级 Agent Evaluation Platform。

## Product

- 登录注册；
- 多用户；
- 团队协作；
- 权限体系；
- 社区；
- 评论；
- 发布平台；
- 订阅收费；
- 支付；
- 商业运营后台；
- 完整专业小说编辑器。

## Reliability

- 企业级 SLA；
- 百万字生产验证；
- 高并发；
- 分布式部署；
- Kubernetes；
- 微服务拆分。

除非 v0.1 真实开发过程中出现无法继续的阻塞问题，否则不得突破以上边界。

---

# 15. Technical Scope Principle

v0.1 计划采用：

> **Java 后端作为主要业务系统 + Python 承担适合 AI 实验与编排的能力 + Web Frontend。**

具体技术选择由 `TECH_STACK.md` 定义。

但 MVP Scope 强制要求：

> 不得因为采用 Java + Python 而人为制造复杂微服务架构。

如果某个能力可以通过简单、稳定的方式完成，就优先选择简单实现。

技术栈的目的首先是：

1. 完成产品验证；
2. 保持项目可解释；
3. 为后续求职展示提供真实工程内容。

而不是单纯增加技术关键词数量。

---

# 16. v0.1 Acceptance Scenario

v0.1 至少必须成功完成一次完整演示。

建议使用一个专门的测试故事。

例如：

```text
核心创意：

修仙界天帝穿越到西幻世界。

初始方向：

主角不了解当地世界，
决定隐藏身份并了解这个世界。
```

系统完成：

```text
作者输入阶段方向
↓
AI 生成章节计划
↓
作者接受或修改计划
↓
AI 连续生成多个章节
↓
每章提取并更新 Memory
↓
后续章节读取并使用已有 Memory
↓
完成当前阶段
```

之后作者再输入一个新的阶段方向：

```text
和已经认识的角色一起前往冒险者公会注册，
并安排一次适当的人前显圣。
```

系统继续生成下一阶段。

---

# 17. Minimum Acceptance Criteria

v0.1 至少满足：

### AC-01

同一个 Story Project 连续生成至少 **5 个逻辑连续的章节**。

不要求达到商业网文正式字数标准。

测试阶段允许使用较短章节以降低 API 成本和等待时间。

---

### AC-02

作者不需要为每一章重新输入：

- 世界设定；
- 人物信息；
- 历史剧情；
- 已确认状态。

---

### AC-03

作者至少可以通过一次阶段方向，让 AI 自主生成多个中间章节。

---

### AC-04

AI 能提出建议章节数量。

作者能够调整该数量，并获得重新规划后的章节计划。

---

### AC-05

至少验证：

```text
Continuous Mode
```

和：

```text
Step-by-Step Mode
```

均能运行。

---

### AC-06

每章完成后能够产生 Memory Candidates。

---

### AC-07

Memory Candidates 能够分类为：

```text
AUTO
REVIEW
IGNORE
```

并允许作者覆盖 AI 判断。

---

### AC-08

至少成功维护以下三类 Current State 中的若干实例：

- 地点；
- 物品；
- 情绪 / 身体状态；
- 人物关系。

---

### AC-09

至少保存一个跨章节仍具有意义的 Story Memory。

例如：

- 重要细节；
- 未解释现象；
- 潜在伏笔；
- 重要事件。

---

### AC-10

至少一条 Memory 可以查看对应：

```text
source_chapter
+
evidence_text
```

---

### AC-11

Story Query 至少能够正确回答：

```text
主角当前在哪里？

主角有哪些重要关系？

当前有哪些潜在伏笔？
```

---

### AC-12

Planner 能基于当前故事状态提出至少 **3 个不同的后续剧情方向**。

---

### AC-13

在 5 章测试中，不应出现明显违反已确认 Story Constraints 的情况而完全没有被系统发现或记录。

---

### AC-14

开发者能够完整演示：

```text
Idea
→
Stage Direction
→
Plan
→
Generation
→
Memory Update
→
Next Chapter
→
Query / Planning Assistance
```

形成闭环。

---

# 18. What v0.1 Does NOT Need to Prove

即使以下问题仍然存在，也不代表 v0.1 失败：

- 章节文学质量仍不稳定；
- AI 有时提取错误 Memory；
- AUTO / REVIEW 分类并不完美；
- Memory 数量增长后可能出现新问题；
- 5 章成功不能证明 100 章成功；
- 尚未加入 Vector Retrieval；
- 尚未解决所有历史状态变化；
- Planner 建议偶尔质量不高；
- 人物情绪仍可能偶尔出现不自然变化。

这些问题应该成为：

> **下一阶段实验和迭代输入。**

v0.1 的目标不是消灭所有问题。

而是：

> 建立一个足够真实的系统，让这些问题第一次可以被观察和测量。

---

# 19. Scope Change Rule

在 v0.1 开发期间，任何新增需求必须回答：

1. 这个需求是否直接阻塞核心闭环？
2. 不实现它，v0.1 是否无法完成核心实验？
3. 它解决的是已经出现的问题，还是想象中的未来问题？

只有前两个问题中至少一个明确为“是”，才能考虑加入当前版本。

否则：

> 延后。

开发 Agent 不得自行修改本规则。

---

# 20. Definition of Done

当以下条件全部满足时，可以宣布：

> **v0.1 Complete**

- 核心 Web UI 可以运行；
- 可以创建故事；
- 可以输入阶段方向；
- AI 可以生成并调整章节计划；
- 可以生成至少 5 个连续章节；
- Continuous / Step-by-Step 两种模式可运行；
- 每章能够进行 Memory Extraction；
- 作者可以处理 Memory Candidates；
- Current State 能够影响后续生成；
- Story Memory 能够被后续系统使用；
- Memory 能够追溯来源章节；
- Planner 能提供后续剧情建议；
- Story Query 能回答基本故事状态；
- 至少完成一次完整端到端测试；
- 已知失败和不足被记录，而不是为了“完成”而隐藏。

完成以上条件后：

> **立即停止向 v0.1 增加功能。**

进入实验总结和下一版本规划。