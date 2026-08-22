"""Deterministic mock builders.

Used only when no LLM_API_KEY is configured, so the full pipeline is runnable
and testable offline. They are intentionally simple but produce structurally
valid outputs that satisfy the acceptance contracts.
"""
from __future__ import annotations

import re

from app.schemas.models import (
    DirectionItem,
    ExtractMemoryResponse,
    GenerateChapterResponse,
    MemoryCandidate,
    PlanStageResponse,
    ChapterPlanItem,
    StateItem,
    StoryQueryResponse,
    SuggestDirectionsResponse,
)


def mock_plan(req) -> PlanStageResponse:
    count = req.targetChapterCount or 3
    goals = [
        "推进阶段目标的起始铺垫，建立当前场景与人物处境。",
        "在已建立的环境中出现关键转折或新人物，推动主线。",
        "完成本阶段核心事件，并为下一阶段留下明确接口。",
    ]
    # TASK-115: emit full ChapterSpec so the Mock pipeline exercises the new
    # contract (targetCharacters / mustAdvance / mustNotDo / storyBeats / endingIntent).
    target_chars = 3000
    plans = [
        ChapterPlanItem(
            order=i + 1,
            goal=goals[i % len(goals)],
            expectedProgress=f"完成第 {i + 1}/{count} 章的目标，推进阶段方向。",
            targetCharacters=target_chars,
            mustAdvance=[goals[i % len(goals)], "推进主线，避免原地踏步"],
            mustNotDo=["重复已完成阶段", "重复开场/穿越"],
            storyBeats=["建立处境", "出现转折", "留下接口"],
            endingIntent="为下一章埋下明确接口。",
        )
        for i in range(count)
    ]
    return PlanStageResponse(suggestedChapterCount=count, chapterPlans=plans)


def mock_generate(req) -> GenerateChapterResponse:
    order = req.chapterOrder
    goal = req.chapterGoal
    title = f"第{order}章 · {goal[:12]}"
    summary = f"本章围绕「{goal}」展开。主角在当前处境下继续推进，周围人物与环境的细节被逐步铺开。"
    content = (
        f"第{order}章\n\n"
        f"{summary}\n\n"
        f"主角记得自己来自另一个世界，此刻仍须隐藏身份。{goal}\n\n"
        f"他来到禁书区最深处，终于在一堆残卷之下找到想要的东西。他获得了一把生锈的铜钥匙，握在掌心仍有余温。\n\n"
        f"通道尽头突然塌方，碎石擦过肩头，他受了伤，血顺着臂膀缓缓淌下。\n\n"
        f"艾琳在一旁观察着他，神色间仍有戒备，却也难免流露出几分关切。\n\n"
        f"玉佩贴在胸口，微微发烫，只是他尚未明白其中缘由。\n\n"
        f"（本章为 Mock Provider 生成的占位正文，用于在无 LLM_API_KEY 时验证端到端链路。）"
    )
    return GenerateChapterResponse(title=title, content=content, summary=summary)


def mock_extract(req) -> ExtractMemoryResponse:
    text = req.chapterContent or ""
    candidates: list[MemoryCandidate] = []

    # Location change
    m = re.search(r"(?:来到|前往|进入|抵达)([一-龥]{2,10}?)(?:。|，|公会|城镇|城市|酒馆|营地)", text)
    if m:
        candidates.append(
            MemoryCandidate(
                type="CURRENT_STATE",
                subject="主角",
                field="location",
                value=m.group(1),
                suggestedAction="AUTO",
                evidence=f"正文提及「{m.group(0)}」。",
            )
        )

    # Inventory acquisition
    m = re.search(r"(?:获得|得到|购买|捡到|拾得)(?:了)?(一把|一枚|一本|一个)?([一-龥]{1,8}?)(?:。|，)", text)
    if m:
        candidates.append(
            MemoryCandidate(
                type="CURRENT_STATE",
                subject="主角",
                field="inventory",
                value=m.group(2),
                suggestedAction="AUTO",
                evidence=f"正文提及「{m.group(0)}」。",
            )
        )

    # Physical condition
    if "受伤" in text or "伤" in text:
        candidates.append(
            MemoryCandidate(
                type="CURRENT_STATE",
                subject="主角",
                field="physical_condition",
                value="受伤",
                suggestedAction="AUTO",
                evidence="正文提及伤势。",
            )
        )

    # Relationship: 艾琳
    if "艾琳" in text:
        if "敌意" in text or "警惕" in text or "戒备" in text:
            rel = "艾琳对主角保持警惕/敌意"
            action = "REVIEW"
        else:
            rel = "艾琳对主角的敌意有所降低"
            action = "REVIEW"
        candidates.append(
            MemoryCandidate(
                type="RELATIONSHIP",
                subject="艾琳->主角",
                field=None,
                value=rel,
                suggestedAction=action,
                evidence="正文包含艾琳与主角的互动。",
            )
        )

    # Foreshadowing: 黑袍 / 玉佩
    if "黑袍" in text or ("玉佩" in text and ("停顿" in text or "看" in text)):
        candidates.append(
            MemoryCandidate(
                type="STORY_MEMORY",
                subject="玉佩",
                field=None,
                value="黑袍老人对玉佩表现出异常关注，疑似伏笔。",
                suggestedAction="REVIEW",
                evidence="正文出现黑袍老人注视玉佩的情节。",
            )
        )

    if not candidates:
        candidates.append(
            MemoryCandidate(
                type="STORY_MEMORY",
                subject="本章",
                field=None,
                value=req.chapterSummary or "本章暂未提取到高价值信息。",
                suggestedAction="IGNORE",
                evidence="无明确可提取信号。",
            )
        )
    return ExtractMemoryResponse(candidates=candidates)


def mock_suggest(req) -> SuggestDirectionsResponse:
    return SuggestDirectionsResponse(
        directions=[
            DirectionItem(
                title="冲突型",
                description="让主角在关键场合暴露部分非常规力量，引发当地势力关注与新的冲突。",
            ),
            DirectionItem(
                title="成长型",
                description="通过一次任务或考验，让主角逐步了解本世界规则，并与重要配角建立信任。",
            ),
            DirectionItem(
                title="悬疑型",
                description="围绕玉佩与神秘人物的线索展开，埋下长期伏笔而不立即揭晓。",
            ),
        ]
    )


def mock_query(req) -> StoryQueryResponse:
    q = req.question
    # Try to answer from current state / memories when possible.
    for s in req.currentState:
        if s.field == "location" and ("在哪里" in q or "位置" in q or "何处" in q):
            return StoryQueryResponse(answer=f"当前位置：{s.value}。")
        if s.field == "inventory" and ("物品" in q or "持有" in q or "装备" in q):
            return StoryQueryResponse(answer=f"当前持有：{s.value}。")
    if "关系" in q or "艾琳" in q:
        for r in req.relationshipState:
            if "艾琳" in r.subjectA or "艾琳" in r.subjectB:
                return StoryQueryResponse(answer=f"关系状态：{r.description}。")
    if "伏笔" in q:
        fs = [m.description for m in req.storyMemories if "伏笔" in m.description or m.type == "STORY_MEMORY"]
        if fs:
            return StoryQueryResponse(answer="已知伏笔：" + "；".join(fs))
    return StoryQueryResponse(
        answer="当前故事中没有确定这一信息（未知）。"
    )
