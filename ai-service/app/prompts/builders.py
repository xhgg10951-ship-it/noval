"""Prompt construction for each AI capability.

Each builder returns a (system, user) prompt tuple. The system prompt pins the
role and the strict JSON output contract; the user prompt serializes the
structured inputs so the LLM has everything it needs to produce a valid output.
"""
from __future__ import annotations

from typing import Tuple

from app.schemas.models import (
    ExtractMemoryRequest,
    GenerateChapterRequest,
    PlanStageRequest,
    StoryQueryRequest,
    SuggestDirectionsRequest,
)

import logging

_request_logger = logging.getLogger("ai.requests")


def log_request_shape(name: str, req) -> None:
    """DEBUG-only observability (TASK-103).

    Logs which context fields are present vs empty so wiring gaps
    (e.g. empty currentState / storyMemories) are visible without a
    debugger. Request DTOs carry no API keys, so no secret is logged.
    """
    if not _request_logger.isEnabledFor(logging.DEBUG):
        return

    def _len(v):
        if v is None:
            return 0
        if isinstance(v, (list, str)):
            return len(v)
        return 1

    shape = {
        "coreIdea": _len(getattr(req, "coreIdea", None)),
        "constraints": _len(getattr(req, "constraints", None)),
        "stageDirection": _len(getattr(req, "stageDirection", None)),
        "chapterGoal": _len(getattr(req, "chapterGoal", None)),
        "currentState": _len(getattr(req, "currentState", None)),
        "storyMemories": _len(getattr(req, "storyMemories", None)),
        "relationshipState": _len(getattr(req, "relationshipState", None)),
        "recentContext": _len(getattr(req, "recentContext", None)),
        "targetChapterCount": getattr(req, "targetChapterCount", None),
        "continuationAnchor": _len(getattr(req, "continuationAnchor", None)),
        "completedStageSummaries": _len(getattr(req, "completedStageSummaries", None)),
        "recentChapterSummaries": _len(getattr(req, "recentChapterSummaries", None)),
        "currentChapterNumber": getattr(req, "currentChapterNumber", None),
    }
    _request_logger.debug("[%s] received request shape: %s", name, shape)


def _fmt_constraints(items) -> str:
    return "\n".join(f"- [{i.type}] {i.content}" for i in items) or "(无)"


def _fmt_state(items) -> str:
    return (
        "\n".join(
            f"- [{i.category}] {i.subject}{('/' + i.field) if i.field else ''}: {i.value}"
            for i in items
        )
        or "(无)"
    )


def _fmt_memories(items) -> str:
    return "\n".join(f"- [{i.type}] {i.subject or ''}: {i.description}" for i in items) or "(无)"


def _fmt_relationships(items) -> str:
    return "\n".join(f"- {i.subjectA} <-> {i.subjectB}: {i.description}" for i in items) or "(无)"


PLAN_OUTPUT_HINT = (
    '{"suggestedChapterCount": int, '
    '"chapterPlans": [{"order": int, "goal": str, "expectedProgress": str, '
    '"targetCharacters": int, "mustAdvance": [str], "mustNotDo": [str], '
    '"storyBeats": [str], "endingIntent": str}]}'
)


def build_plan_prompt(req: PlanStageRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说分章策划 AI。根据作者的'阶段导演指令'、故事约束、当前状态与记忆，"
        "输出一个分阶段章节计划。必须只返回严格 JSON，格式为：\n" + PLAN_OUTPUT_HINT
    )
    # TASK-109: continuation framing. If the story already has chapters, the
    # Planner is CONTINUING an existing story and must not restart it.
    anchor_block = _fmt_continuation(req)
    user = f"""核心创意：
{req.coreIdea}

阶段导演指令：
{req.stageDirection}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

{anchor_block}

近期上下文：
{req.recentContext or '(无)'}

已完成阶段概要：
{_fmt_lines(req.completedStageSummaries)}

近期章节摘要：
{_fmt_lines(req.recentChapterSummaries)}

目标章节数：{req.targetChapterCount or '由你决定'}

每一章计划都必须包含完整的 ChapterSpec：
- targetCharacters：本章目标字数（建议 2500–3500，长篇请接近上限）
- mustAdvance：本章必须推进的要点列表
- mustNotDo：本章禁止做的事列表（例如不得重复已完成阶段、不得重复开场）
- storyBeats：本章关键剧情节拍列表
- endingIntent：本章结尾意图（为下一章留接口）

请只输出 JSON。"""
    return system, user


def _fmt_lines(items) -> str:
    if not items:
        return "(无)"
    return "\n".join(f"- {line}" for line in items)


def _fmt_continuation(req: PlanStageRequest) -> str:
    """TASK-107/108/109: render the continuation context block.

    When the story has prior chapters, pins the 'you are continuing' framing
    and surfaces the structured anchor so the Planner does not repeat
    already-resolved plot (crossing / first meeting / already-owned identity).
    """
    if req.currentChapterNumber is None:
        return "续写状态：这是故事的第一阶段，没有前情，可以自由开篇。"
    a = req.continuationAnchor
    parts = [
        "续写状态：这是一部正在连载的故事，你必须坚持已有剧情，不得重新开篇。",
        f"当前最新章节编号：第 {req.currentChapterNumber} 章",
    ]
    if a is not None:
        if a.lastChapterSummary:
            parts.append(f"上一章摘要：{a.lastChapterSummary}")
        if a.lastChapterEnding:
            parts.append(f"上一章结尾（节选）：{a.lastChapterEnding}")
        if a.currentLocation:
            parts.append(f"当前地点：{a.currentLocation}")
        if a.activeCharacters:
            parts.append("当前活跃人物：" + "、".join(a.activeCharacters))
        if a.currentImmediateGoal:
            parts.append(f"当前直接目标：{a.currentImmediateGoal}")
    parts.append(
        "禁止：再次描写穿越、初次遇见已经认识的人物、重新获得已经拥有的身份或住所、"
        "重复已经完成过的阶段。新计划必须从上述续写状态继续。"
    )
    return "\n".join(parts)


GENERATE_OUTPUT_HINT = '{"title": str, "content": str, "summary": str}'


def build_generate_prompt(req: GenerateChapterRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说章节写作 AI。根据阶段导演指令、本章目标与上下文，写出一章连贯的叙事正文，"
        "并附标题与摘要。必须只返回严格 JSON，格式为：\n" + GENERATE_OUTPUT_HINT
    )
    # TASK-118: Goal Lock — enforce priority order and Must-Not compliance.
    spec_block = _fmt_writer_spec(req)
    user = f"""核心创意：
{req.coreIdea}

阶段导演指令：
{req.stageDirection}

{spec_block}

本章目标（第 {req.chapterOrder} 章）：
{req.chapterGoal}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

【执行优先级，必须自上而下遵守】
1. 硬性约束（约束列表）高于一切；
2. 本章目标（ChapterSpec）是本章必须执行的主线；
3. 必须推进（mustAdvance）必须发生；禁止事项（mustNotDo）绝不可违反；
4. 故事记忆用于丰富细节，不得用它替换本章主线；
5. 与本章无关的旧细节不要重复铺陈；
6. 本章结尾意图（endingIntent）应被满足，为下一章留接口。

请只输出 JSON。"""
    return system, user


def _fmt_writer_spec(req: GenerateChapterRequest) -> str:
    """TASK-117/118: render the full ChapterSpec the Writer must execute."""
    lines = ["本章执行规格（ChapterSpec）："]
    if req.targetCharacters is not None:
        lines.append(f"- 目标字数：约 {req.targetCharacters} 字（请尽量接近，不要严重偏短）")
    if req.mustAdvance:
        lines.append(f"- 必须推进：{req.mustAdvance}")
    if req.mustNotDo:
        lines.append(f"- 禁止事项：{req.mustNotDo}（绝不可违反）")
    if req.storyBeats:
        lines.append(f"- 剧情节拍：{req.storyBeats}")
    if req.endingIntent:
        lines.append(f"- 结尾意图：{req.endingIntent}")
    if len(lines) == 1:
        lines.append("- （无显式规格，按本章目标自由发挥）")
    return "\n".join(lines)


EXTRACT_OUTPUT_HINT = (
    '{"candidates": [{"type": str, "subject": str, "field": str|null, '
    '"value": str, "suggestedAction": "AUTO"|"REVIEW"|"IGNORE", "evidence": str}]}'
)


def build_extract_prompt(req: ExtractMemoryRequest) -> Tuple[str, str]:
    system = (
        "你是一名故事记忆抽取 AI。从章节正文与摘要中，抽取应当被长期记住的事实："
        "当前状态(CURRENT_STATE)、人物关系(RELATIONSHIP)、故事记忆(STORY_MEMORY，含伏笔)。"
        "每个候选给出 subject、可选 field、value、suggestedAction(AUTO 自动采纳 / REVIEW 需作者确认 / IGNORE 忽略)、"
        "以及 evidence 证据原文。必须只返回严格 JSON，格式为：\n" + EXTRACT_OUTPUT_HINT
    )
    user = f"""章节摘要：
{req.chapterSummary}

章节正文：
{req.chapterContent}

已有状态：
{_fmt_state(req.existingState)}

约束：
{_fmt_constraints(req.constraints)}

请只输出 JSON。"""
    return system, user


SUGGEST_OUTPUT_HINT = '{"directions": [{"title": str, "description": str}]}'


def build_suggest_prompt(req: SuggestDirectionsRequest) -> Tuple[str, str]:
    system = (
        "你是一名创作辅助 AI。基于当前故事状态与约束，给出若干条值得作者考虑的下一步创作方向，"
        "例如冲突型、成长型、悬疑型。必须只返回严格 JSON，格式为：\n" + SUGGEST_OUTPUT_HINT
    )
    user = f"""核心创意：
{req.coreIdea}

约束：
{_fmt_constraints(req.constraints)}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

请只输出 JSON。"""
    return system, user


QUERY_OUTPUT_HINT = '{"answer": str}'


def build_query_prompt(req: StoryQueryRequest) -> Tuple[str, str]:
    system = (
        "你是一名故事问答 AI。基于当前状态、故事记忆与人物关系，回答作者关于'故事现在是什么情况'的问题。"
        "若信息不足，明确说明'当前故事中没有确定这一信息（未知）'。必须只返回严格 JSON，格式为：\n"
        + QUERY_OUTPUT_HINT
    )
    user = f"""问题：
{req.question}

当前状态：
{_fmt_state(req.currentState)}

故事记忆：
{_fmt_memories(req.storyMemories)}

人物关系：
{_fmt_relationships(req.relationshipState)}

近期上下文：
{req.recentContext or '(无)'}

请只输出 JSON。"""
    return system, user
