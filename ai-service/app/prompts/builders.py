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
    '"chapterPlans": [{"order": int, "goal": str, "expectedProgress": str}]}'
)


def build_plan_prompt(req: PlanStageRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说分章策划 AI。根据作者的'阶段导演指令'、故事约束、当前状态与记忆，"
        "输出一个分阶段章节计划。必须只返回严格 JSON，格式为：\n" + PLAN_OUTPUT_HINT
    )
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

近期上下文：
{req.recentContext or '(无)'}

目标章节数：{req.targetChapterCount or '由你决定'}

请只输出 JSON。"""
    return system, user


GENERATE_OUTPUT_HINT = '{"title": str, "content": str, "summary": str}'


def build_generate_prompt(req: GenerateChapterRequest) -> Tuple[str, str]:
    system = (
        "你是一名小说章节写作 AI。根据阶段导演指令、本章目标与上下文，写出一章连贯的叙事正文，"
        "并附标题与摘要。必须只返回严格 JSON，格式为：\n" + GENERATE_OUTPUT_HINT
    )
    user = f"""核心创意：
{req.coreIdea}

阶段导演指令：
{req.stageDirection}

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

请只输出 JSON。"""
    return system, user


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
